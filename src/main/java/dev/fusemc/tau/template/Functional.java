package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.TypeException;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Origin;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public final class Functional<T> implements Template<T> {

    private final @NotNull Class<T> type;
    private final @NotNull Template<?> template;
    private final @NotNull Method target;

    @SuppressWarnings("PatternVariableHidesField")
    public Functional(@NotNull Class<T> type,
                      @NotNull Template<?> template) {
        var option = Functional.findTarget(type);
        if (option instanceof Option.Some<Method>(var target)) {
            this.type = Objects.requireNonNull(type);
            this.template = Objects.requireNonNull(template);
            this.target = target;
            return;
        }
        throw new AssertionError();
    }

    @ApiStatus.Internal
    public static @NotNull Option<Method> findTarget(@NotNull Class<?> type) {
        Objects.requireNonNull(type);
        if (type.isInterface()) {
            var haystack = type.getMethods();
            var needle = (Method) null;
            for (var method : haystack) {
                var flags = method.accessFlags();
                if (flags.contains(AccessFlag.ABSTRACT)) {
                    if (needle == null) {
                        needle = method;
                        continue;
                    }
                    return Option.none();
                }
            }
            if (needle != null)
                return Option.some(needle);
            return Option.none();
        }
        return Option.none();
    }

    @ApiStatus.Internal
    private static @NotNull Description describe(@NotNull Type type,
                                                 boolean spread) {
        Objects.requireNonNull(type);
        var raw = Functional.raw(type);
        if (raw == Byte.class || raw == byte.class)
            return Description.BYTE;
        if (raw == Short.class || raw == short.class)
            return Description.SHORT;
        if (raw == Integer.class || raw == int.class)
            return Description.INTEGER;
        if (raw == Long.class || raw == long.class)
            return Description.LONG;
        if (raw == Float.class || raw == float.class)
            return Description.FLOAT;
        if (raw == Double.class || raw == double.class)
            return Description.DOUBLE;
        if (raw == Boolean.class || raw == boolean.class)
            return Description.BOOLEAN;
        if (raw == Void.class || raw == void.class)
            return Description.UNDEFINED;
        if (raw == BigInteger.class)
            return Description.BIG_INTEGER;
        if (raw == String.class)
            return Description.STRING;
        if (raw == Object.class || raw == Value.class)
            return Description.ANY;
        if (raw.isArray()) {
            var component = Functional.describe(
                    type instanceof GenericArrayType gat
                            ? gat.getGenericComponentType()
                            : raw.getComponentType(),
                    false
            );
            if (spread)
                return Description.concat(Description.ELLIPSIS, component);
            return Description.concat(component, Description.delimiter("[]"));
        }
        if (type instanceof ParameterizedType pt) {
            var generics = pt.getActualTypeArguments();
            return Description.concat(
                    Description.reference(raw),
                    Description.concat(
                            Description.delimiter('<'),
                            Description.join(Description.delimiter(", "), Arrays.stream(generics)
                                    .map(t -> Functional.describe(t, false))
                                    .toArray(Description[]::new)),
                            Description.delimiter('>')
                    )
            );
        }
        return Description.reference(raw);
    }

    @ApiStatus.Internal
    private static @NotNull Class<?> raw(@NotNull Type type) {
        return switch (type) {
            case Class<?> clazz -> clazz;
            case ParameterizedType pt -> Functional.raw(pt.getRawType());
            case TypeVariable<?> tv -> {
                var bounds = tv.getBounds();
                if (bounds.length == 1)
                    yield Functional.raw(bounds[0]);
                yield Object.class;
            }
            case WildcardType wt -> {
                var upper = wt.getUpperBounds();
                if (upper.length == 1)
                    yield Functional.raw(upper[0]);
                yield Object.class;
            }
            case GenericArrayType gat -> Array.newInstance(
                    Functional.raw(gat.getGenericComponentType()),
                    0
            ).getClass();
            default -> Object.class;
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Option<T> lower(@NotNull Value value) {
        if (value.canExecute()) {
            var handler = new Handler(this.target, this.template, value);
            return Option.some((T) Proxy.newProxyInstance(
                    this.type.getClassLoader(),
                    new Class<?>[]{this.type},
                    handler
            ));
        }
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (this.type.isInstance(host))
                return Option.some(this.type.cast(host));
            return Option.none();
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null) {
            var type = value.getClass();
            if (Proxy.isProxyClass(type)) {
                var handler = Proxy.getInvocationHandler(value);
                if (handler instanceof Handler(var _, var _, var implementation))
                    return Option.some(implementation);
                return Option.none();
            }
            return Option.some(Value.asValue(value));
        }
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Description.concat(
                Description.concat(
                        Description.delimiter('('),
                        Description.join(Description.delimiter(", "), Arrays.stream(this.target.getParameters())
                                .map(parameter -> Functional.describe(
                                        parameter.getParameterizedType(),
                                        parameter.isVarArgs()
                                ))
                                .toArray(Description[]::new)),
                        Description.delimiter(')')
                ),
                Description.delimiter(" => "),
                this.template.describe(points)
        ), Origin.SCHEMA);
    }

    private record Handler(@NotNull Method target, @NotNull Template<?> template,
                           @NotNull Value implementation) implements InvocationHandler {

        private static final Method TO_STRING;
        private static final Method HASH_CODE;
        private static final Method EQUALS;

        private Handler(@NotNull Method target,
                        @NotNull Template<?> template,
                        @NotNull Value implementation) {
            this.target = Objects.requireNonNull(target);
            this.template = Objects.requireNonNull(template);
            this.implementation = Objects.requireNonNull(implementation);
        }

        @ApiStatus.Internal
        private static Object @NotNull [] box(Object array) {
            return switch (array) {
                case byte[] bytes -> {
                    var buffer = new Byte[bytes.length];
                    for (var i = 0; i < bytes.length; i++)
                        buffer[i] = bytes[i];
                    yield buffer;
                }
                case short[] shorts -> {
                    var buffer = new Short[shorts.length];
                    for (var i = 0; i < shorts.length; i++)
                        buffer[i] = shorts[i];
                    yield buffer;
                }
                case int[] ints -> {
                    var buffer = new Integer[ints.length];
                    for (var i = 0; i < ints.length; i++)
                        buffer[i] = ints[i];
                    yield buffer;
                }
                case long[] longs -> {
                    var buffer = new Long[longs.length];
                    for (var i = 0; i < longs.length; i++)
                        buffer[i] = longs[i];
                    yield buffer;
                }
                case float[] floats -> {
                    var buffer = new Float[floats.length];
                    for (var i = 0; i < floats.length; i++)
                        buffer[i] = floats[i];
                    yield buffer;
                }
                case double[] doubles -> {
                    var buffer = new Double[doubles.length];
                    for (var i = 0; i < doubles.length; i++)
                        buffer[i] = doubles[i];
                    yield buffer;
                }
                case boolean[] booleans -> {
                    var buffer = new Boolean[booleans.length];
                    for (var i = 0; i < booleans.length; i++)
                        buffer[i] = booleans[i];
                    yield buffer;
                }
                case char[] chars -> {
                    var buffer = new Character[chars.length];
                    for (var i = 0; i < chars.length; i++)
                        buffer[i] = chars[i];
                    yield buffer;
                }
                case Object[] objects -> objects;
                default -> throw new AssertionError();
            };
        }

        @ApiStatus.Internal
        @SuppressWarnings("unchecked")
        private static <T> @NotNull Option<T> cast(@NotNull Class<T> clazz, @NotNull Object value) {
            if (clazz == byte.class) {
                if (value instanceof Byte b)
                    return Option.some((T) b);
                return Option.none();
            }
            if (clazz == short.class) {
                if (value instanceof Short s)
                    return Option.some((T) s);
                return Option.none();
            }
            if (clazz == int.class) {
                if (value instanceof Integer i)
                    return Option.some((T) i);
                return Option.none();
            }
            if (clazz == long.class) {
                if (value instanceof Long l)
                    return Option.some((T) l);
                return Option.none();
            }
            if (clazz == float.class) {
                if (value instanceof Float f)
                    return Option.some((T) f);
                return Option.none();
            }
            if (clazz == double.class) {
                if (value instanceof Double d)
                    return Option.some((T) d);
                return Option.none();
            }
            if (clazz == boolean.class) {
                if (value instanceof Boolean b)
                    return Option.some((T) b);
                return Option.none();
            }
            if (clazz == char.class) {
                if (value instanceof Character c)
                    return Option.some((T) c);
                return Option.none();
            }
            if (clazz.isInstance(value))
                return Option.some(clazz.cast(value));
            return Option.none();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.equals(Handler.TO_STRING))
                return this.implementation.toString();
            if (method.equals(Handler.HASH_CODE))
                return this.implementation.hashCode();
            if (method.equals(Handler.EQUALS)) {
                var other = args[0];
                if (other instanceof Proxy p)
                    return this.equals(Proxy.getInvocationHandler(p));
                return this.implementation.equals(other);
            }
            if (method.equals(this.target)) {
                var value = Tau.lower(this.template, this.implementation.execute(this.spreadVariadic(args)));
                var option = Handler.cast(this.target.getReturnType(), value);
                if (option instanceof Option.Some<?>(var result))
                    return result;
                throw new TypeException(Tau.describe(value), Functional.describe(this.target.getGenericReturnType(), false));
            }
            throw new AssertionError();
        }

        @ApiStatus.Internal
        private Object @NotNull [] spreadVariadic(Object @NotNull [] args) {
            if (this.target.isVarArgs()) {
                var variadic = Handler.box(args[args.length - 1]);
                var buffer = Arrays.copyOf(args, args.length + variadic.length - 1);
                System.arraycopy(variadic, 0, buffer, args.length - 1, variadic.length);
                return buffer;
            }
            return args;
        }

        @Override
        @SuppressWarnings("PatternVariableHidesField")
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof Handler(var target, var template, var implementation))
                return this.target.equals(target)
                        && this.template.equals(template)
                        && this.implementation.equals(implementation);
            return false;
        }

        static {
            try {
                TO_STRING = Object.class.getDeclaredMethod("toString");
                HASH_CODE = Object.class.getDeclaredMethod("hashCode");
                EQUALS = Object.class.getDeclaredMethod("equals", Object.class);
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }
    }
}
