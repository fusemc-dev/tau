package dev.fusemc.tau.proxy;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.TypeException;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;

/// Represents a function-like.
///
/// ---
/// Defines an [InvocationHandler] that acts as an implementation
/// of a **functional** interface in terms of a Polyglot [Value].
///
/// @since 0.1.0
public final class FunctionLike implements InvocationHandler {

    private static final Method TO_STRING;
    private static final Method HASH_CODE;
    private static final Method EQUALS;

    private final @NotNull Method target;
    private final @NotNull Value delegate;
    private final @Nullable Template<?> template;

    public FunctionLike(@NotNull Method target,
                        @NotNull Value delegate,
                        @Nullable Template<?> template) {
        this.target   = Objects.requireNonNull(target);
        this.delegate = Objects.requireNonNull(delegate);
        this.template = template;
    }

    @ApiStatus.Internal
    private static Object @NotNull [] box(@NotNull Object array) {
        Objects.requireNonNull(array);
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
    private static <T> @NotNull Option<T> cast(@NotNull Class<T> clazz, @Nullable Object value) {
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
        if (clazz == void.class) {
            if (value == null)
                return Option.some(null);
            return Option.none();
        }
        if (clazz.isInstance(value))
            return Option.some(clazz.cast(value));
        return Option.none();
    }

    @Override
    public Object invoke(@NotNull Object proxy,
                         @NotNull Method method,
                         Object[] args) throws Throwable {
        Objects.requireNonNull(proxy);
        Objects.requireNonNull(method);
        if (method.equals(FunctionLike.TO_STRING))
            return this.delegate.toString();
        if (method.equals(FunctionLike.HASH_CODE))
            return this.delegate.hashCode();
        if (method.equals(FunctionLike.EQUALS)) {
            var other = args[0];
            if (other instanceof Proxy p)
                return this.equals(Proxy.getInvocationHandler(p));
            return false;
        }
        if (method.equals(this.target)) {
            var result = this.delegate.execute(args == null ? new Object[0] : this.spreadVariadic(args));
            if (this.template != null) {
                var value = Tau.lower(this.template, result);
                var option = FunctionLike.cast(this.target.getReturnType(), value);
                if (option instanceof Option.Some<?>(var wrapped))
                    return wrapped;
                throw new TypeException(Tau.describe(value), Tau.describe(this.target.getGenericReturnType()));
            }
            return result;
        }
        if (method.isDefault()) {
            var lookup         = MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup());
            var implementation = lookup.unreflectSpecial(method, method.getDeclaringClass()).bindTo(proxy);
            if (args == null)
                return implementation.invokeWithArguments();
            return implementation.invokeWithArguments(args);
        }
        throw new AssertionError();
    }

    @ApiStatus.Internal
    private Object @NotNull [] spreadVariadic(Object @NotNull [] args) {
        if (this.target.isVarArgs()) {
            var variadic = FunctionLike.box(args[args.length - 1]);
            var buffer = Arrays.copyOf(args, args.length + variadic.length - 1);
            System.arraycopy(variadic, 0, buffer, args.length - 1, variadic.length);
            return buffer;
        }
        return args;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof FunctionLike other)
            return this.target.equals(other.target)
                    && this.delegate.equals(other.delegate)
                    && Objects.equals(this.template, other.template);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.target, this.template, this.delegate);
    }

    public @NotNull Value delegate() {
        return this.delegate;
    }

    static {
        try {
            TO_STRING = Object.class.getDeclaredMethod("toString");
            HASH_CODE = Object.class.getDeclaredMethod("hashCode");
            EQUALS    = Object.class.getDeclaredMethod("equals", Object.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }
}
