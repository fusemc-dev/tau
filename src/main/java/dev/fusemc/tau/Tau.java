package dev.fusemc.tau;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Origin;
import dev.fusemc.tau.proxy.Dictionary;
import dev.fusemc.tau.template.Functional;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.*;
import java.util.function.IntBinaryOperator;

/// The Tau's entrypoint.
///
/// **Tau** (τ) is a runtime [Polyglot](https://www.graalvm.org/latest/reference-manual/polyglot-programming/) [Value] type-validation library, built
/// originally for [Fuse](https://fusemc.dev).
///
/// @since `0.1.0`
public final class Tau {

    @ApiStatus.Internal
    private static final @Nullable Object UNDEFINED_SENTINEL = Tau.loadUndefined();
    private static final int TUPLE_LENGTH_THRESHOLD = 5;

    private Tau() {
        throw new UnsupportedOperationException();
    }

    public static <T> T lower(@NotNull Template<T> template, @NotNull Value value) {
        Objects.requireNonNull(template);
        Objects.requireNonNull(value);
        var option = template.lower(value);
        if (option instanceof Option.Some<T>(var result))
            return result;
        throw new TypeException(Tau.describe(value), template.describe(Scope.hashScope()));
    }

    /// Attempts to raise the provided [T] using the provided
    /// [Template].
    ///
    /// If the provided `value` does not satisfy the requested [Template],
    /// a [TypeException] is thrown in the form of:
    ///
    /// ```
    /// Type '...' is not assignable to type '...'.
    /// ```
    ///
    /// @since `0.1.0`
    /// @see Template
    /// @see TypeException
    public static <T> Value raise(@NotNull Template<T> template, @Nullable T value) {
        Objects.requireNonNull(template);
        var option = template.raise(value);
        if (option instanceof Option.Some<Value>(var result))
            return result;
        throw new TypeException(Tau.describe(value), template.describe(Scope.hashScope()));
    }

    /// Returns an [`undefined`](https://tc39.es/ecma262/#sec-ecmascript-language-types-undefined-type) [Value].
    ///
    /// If the `undefined` sentinel couldn't be accessed at runtime,
    /// the method degrades to returning `Value.asValue(null)`.
    ///
    /// @since `0.1.0`
    /// @see #isUndefined(Value)
    public static @NotNull Value undefined() {
        return Value.asValue(Tau.UNDEFINED_SENTINEL);
    }

    /// Determines whether the provided [Value] is explicitly [`undefined`](https://tc39.es/ecma262/#sec-ecmascript-language-types-undefined-type).
    ///
    /// If the `undefined` sentinel couldn't be accessed at runtime,
    /// the method degrades to always returning `false`.
    ///
    /// @since `0.1.0`
    /// @see #isNull(Value)
    /// @see #undefined()
    public static boolean isUndefined(@NotNull Value value) {
        Objects.requireNonNull(value);
        if (Tau.UNDEFINED_SENTINEL != null)
            return value.isNull() && value.hashCode() == Tau.UNDEFINED_SENTINEL.hashCode();
        return false;
    }

    /// Determines whether the provided `Value` is explicitly `null`.
    ///
    /// This method differs from [Value#isNull()] in that it doesn't consider
    /// [`undefined`](https://tc39.es/ecma262/#sec-ecmascript-language-types-undefined-type)
    /// a "_nullable_" value. If the `undefined` sentinel couldn't be accessed at runtime,
    /// the method degrades to functioning identically to `Value.isNull()`.
    ///
    /// @since `0.1.0`
    /// @see #isUndefined(Value)
    public static boolean isNull(@NotNull Value value) {
        Objects.requireNonNull(value);
        if (Tau.UNDEFINED_SENTINEL != null)
            return value.isNull() && value.hashCode() != Tau.UNDEFINED_SENTINEL.hashCode();
        return value.isNull();
    }

    public static @NotNull Description describe(@Nullable Object o) {
        return Tau.describe(o, Scope.hashScope(), true);
    }

    private static @NotNull Description describe(@Nullable Object o,
                                                 @NotNull Scope<@NotNull Object> visited,
                                                 boolean constant) {
        if (o != null) {
            if (o instanceof Number) {
                if (constant) {
                    return switch (o) {
                        case Byte _    -> Description.attach(Description.BYTE,    Origin.HOST);
                        case Short _   -> Description.attach(Description.SHORT,   Origin.HOST);
                        case Integer _ -> Description.attach(Description.INTEGER, Origin.HOST);
                        case Long _    -> Description.attach(Description.LONG,    Origin.HOST);
                        case Float _   -> Description.attach(Description.FLOAT,   Origin.HOST);
                        case Double _  -> Description.attach(Description.DOUBLE,  Origin.HOST);
                        default -> Description.attach(Description.NUMBER,  Origin.HOST);
                    };
                }
                return Description.attach(Description.NUMBER, Origin.HOST);
            }
            if (o instanceof String literal) {
                if (constant)
                    return Description.attach(Description.literal(literal), Origin.HOST);
                return Description.attach(Description.STRING, Origin.HOST);
            }
            if (o instanceof Boolean)
                return Description.attach(Description.BOOLEAN, Origin.HOST);
            if (o instanceof byte[] bytes) {
                if (bytes.length > 0 && bytes.length < Tau.TUPLE_LENGTH_THRESHOLD) {
                    var buffer = new Description[bytes.length];
                    Arrays.fill(buffer, Description.BYTE);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Origin.HOST);
                }
                return Description.attach(Description.concat(
                        Description.BYTE,
                        Description.delimiter("[]")
                ), Origin.HOST);
            }
            if (o instanceof short[] shorts) {
                if (shorts.length > 0 && shorts.length < Tau.TUPLE_LENGTH_THRESHOLD) {
                    var buffer = new Description[shorts.length];
                    Arrays.fill(buffer, Description.SHORT);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Origin.HOST);
                }
                return Description.attach(Description.concat(
                        Description.SHORT,
                        Description.delimiter("[]")
                ), Origin.HOST);
            }
            if (o instanceof int[] ints) {
                if (ints.length > 0 && ints.length < Tau.TUPLE_LENGTH_THRESHOLD) {
                    var buffer = new Description[ints.length];
                    Arrays.fill(buffer, Description.INTEGER);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Origin.HOST);
                }
                return Description.attach(Description.concat(
                        Description.INTEGER,
                        Description.delimiter("[]")
                ), Origin.HOST);
            }
            if (o instanceof long[] longs) {
                if (longs.length > 0 && longs.length < Tau.TUPLE_LENGTH_THRESHOLD) {
                    var buffer = new Description[longs.length];
                    Arrays.fill(buffer, Description.LONG);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Origin.HOST);
                }
                return Description.attach(Description.concat(
                        Description.LONG,
                        Description.delimiter("[]")
                ), Origin.HOST);
            }
            if (o instanceof float[] floats) {
                if (floats.length > 0 && floats.length < Tau.TUPLE_LENGTH_THRESHOLD) {
                    var buffer = new Description[floats.length];
                    Arrays.fill(buffer, Description.FLOAT);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Origin.HOST);
                }
                return Description.attach(Description.concat(
                        Description.FLOAT,
                        Description.delimiter("[]")
                ), Origin.HOST);
            }
            if (o instanceof double[] doubles) {
                if (doubles.length > 0 && doubles.length < Tau.TUPLE_LENGTH_THRESHOLD) {
                    var buffer = new Description[doubles.length];
                    Arrays.fill(buffer, Description.DOUBLE);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Origin.HOST);
                }
                return Description.attach(Description.concat(
                        Description.DOUBLE,
                        Description.delimiter("[]")
                ), Origin.HOST);
            }
            if (o instanceof boolean[] booleans) {
                if (booleans.length > 0 && booleans.length < Tau.TUPLE_LENGTH_THRESHOLD) {
                    var buffer = new Description[booleans.length];
                    Arrays.fill(buffer, Description.BOOLEAN);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Origin.HOST);
                }
                return Description.attach(Description.concat(
                        Description.BOOLEAN,
                        Description.delimiter("[]")
                ), Origin.HOST);
            }
            if (o instanceof Object[] os) {
                if (visited.add(o)) {
                    if (os.length == 0)
                        return Description.attach(Description.concat(
                                Description.ANY,
                                Description.delimiter("[]")
                        ), Origin.HOST);
                    if (os.length < Tau.TUPLE_LENGTH_THRESHOLD) {
                        var buffer = new Description[os.length];
                        for (var i = 0; i < os.length; i++)
                            buffer[i] = Tau.describe(os[i], visited.branch(), constant);
                        return Description.attach(Description.concat(
                                Description.delimiter('['),
                                Description.join(
                                        Description.delimiter(", "),
                                        buffer
                                ),
                                Description.delimiter(']')
                        ), Origin.HOST);
                    }
                    var buffer = new LinkedHashSet<Description>();
                    for (var element : os)
                        buffer.add(Tau.describe(element, visited.branch(), false));
                    return Description.attach(Description.concat(
                            Description.concat(
                                    Description.delimiter('('),
                                    Description.join(
                                            Description.delimiter(" | "),
                                            buffer.toArray(Description[]::new)
                                    ),
                                    Description.delimiter(')')
                            ),
                            Description.delimiter("[]")
                    ), Origin.HOST);
                }
                return Description.attach(Description.ELLIPSIS, Origin.HOST);
            }
            if (o instanceof Value value)
                return Description.attach(Tau.describe(value, visited, constant), Origin.HOST);
            if (o instanceof Type type)
                return Tau.describe(type);
            return Description.attach(Description.reference(o.getClass()), Origin.HOST);
        }
        return Description.attach(Description.NULL, Origin.HOST);
    }

    private static @NotNull Description describe(@NotNull Value value) {
        return Tau.describe(value, Scope.hashScope(), true);
    }

    private static @NotNull Description describe(@NotNull Value value,
                                                 @NotNull Scope<@NotNull Object> visited,
                                                 boolean constant) {
        if (value.isNumber()) {
            if (constant) {
                if (value.fitsInByte())
                    return Description.attach(Description.BYTE, Origin.POLYGLOT);
                if (value.fitsInShort())
                    return Description.attach(Description.SHORT, Origin.POLYGLOT);
                if (value.fitsInInt())
                    return Description.attach(Description.INTEGER, Origin.POLYGLOT);
                if (value.fitsInLong())
                    return Description.attach(Description.LONG, Origin.POLYGLOT);
                if (value.fitsInFloat())
                    return Description.attach(Description.FLOAT, Origin.POLYGLOT);
                if (value.fitsInDouble())
                    return Description.attach(Description.DOUBLE, Origin.POLYGLOT);
                return Description.attach(Description.NUMBER, Origin.POLYGLOT);
            }
            return Description.attach(Description.NUMBER, Origin.POLYGLOT);
        }
        if (value.isString()) {
            if (constant)
                return Description.attach(Description.literal(value.asString()), Origin.POLYGLOT);
            return Description.attach(Description.STRING, Origin.POLYGLOT);
        }
        if (value.isBoolean())
            return Description.attach(Description.BOOLEAN, Origin.POLYGLOT);
        if (Tau.isUndefined(value))
            return Description.attach(Description.UNDEFINED, Origin.POLYGLOT);
        if (Tau.isNull(value))
            return Description.attach(Description.NULL, Origin.POLYGLOT);
        if (value.hasArrayElements()) {
            if (visited.add(value)) {
                var length = (int) value.getArraySize();
                if (length == 0)
                    return Description.attach(Description.concat(
                            Description.ANY,
                            Description.delimiter("[]")
                    ), Origin.POLYGLOT);
                if (length < Tau.TUPLE_LENGTH_THRESHOLD) {
                    var buffer = new Description[length];
                    for (var i = 0; i < length; i++) {
                        var element = value.getArrayElement(i);
                        buffer[i] = Tau.describe(element, visited.branch(), constant);
                    }
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Origin.POLYGLOT);
                }
                var buffer = new LinkedHashSet<Description>();
                for (var i = 0; i < length; i++) {
                    var element = value.getArrayElement(i);
                    buffer.add(Tau.describe(element, visited.branch(), false));
                }
                return Description.attach(Description.concat(
                        Description.concat(
                                Description.delimiter('('),
                                Description.join(
                                        Description.delimiter(" | "),
                                        buffer.toArray(Description[]::new)
                                ),
                                Description.delimiter(')')
                        ),
                        Description.delimiter("[]")
                ), Origin.POLYGLOT);
            }
            return Description.attach(Description.ELLIPSIS, Origin.POLYGLOT);
        }
        if (value.hasMembers()) {
            if (visited.add(value)) {
                var keys = value.getMemberKeys();
                var buffer = new Description[keys.size()];
                var i = 0;
                for (var key : keys) {
                    var member = value.getMember(key);
                    var description = Tau.describe(member, visited.branch(), constant);
                    buffer[i++] = Description.concat(
                            Description.literal(key),
                            Description.delimiter(": "),
                            description
                    );
                }
                return Description.attach(Description.concat(
                        Description.delimiter('{'),
                        Description.join(
                                Description.delimiter(", "),
                                buffer
                        ),
                        Description.delimiter('}')
                ), Origin.POLYGLOT);
            }
            return Description.attach(Description.ELLIPSIS, Origin.POLYGLOT);
        }
        if (value.isProxyObject()) {
            var proxy = value.asProxyObject();
            if (proxy instanceof Dictionary dictionary) {
                if (visited.add(value)) {
                    var keys = dictionary.getMemberKeys();
                    var buffer = new Description[(int) keys.getSize()];
                    for (var i = 0; i < keys.getSize(); i++) {
                        var key = (String) keys.get(i);
                        var member = dictionary.getMember(key);
                        var description = Tau.describe(member, visited.branch(), constant);
                        buffer[i] = Description.concat(
                                Description.literal(key),
                                Description.delimiter(": "),
                                description
                        );
                    }
                    return Description.attach(Description.concat(
                            Description.delimiter('{'),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter('}')
                    ), Origin.POLYGLOT);
                }
                return Description.attach(Description.ELLIPSIS, Origin.POLYGLOT);
            }
            return Description.attach(Description.UNKNOWN, Origin.POLYGLOT);
        }
        if (value.isHostObject())
            return Description.attach(Tau.describe((Object) value.asHostObject(), visited, constant), Origin.POLYGLOT);
        return Description.attach(Description.UNKNOWN, Origin.POLYGLOT);
    }

    @ApiStatus.Internal
    public static @NotNull Description describe(@NotNull Type type) {
        Objects.requireNonNull(type);
        var raw = Tau.raw(type);
        if (raw == Byte.class || raw == byte.class)
            return Description.attach(Description.BYTE, Origin.REFLECTION);
        if (raw == Short.class || raw == short.class)
            return Description.attach(Description.SHORT, Origin.REFLECTION);
        if (raw == Integer.class || raw == int.class)
            return Description.attach(Description.INTEGER, Origin.REFLECTION);
        if (raw == Long.class || raw == long.class)
            return Description.attach(Description.LONG, Origin.REFLECTION);
        if (raw == Float.class || raw == float.class)
            return Description.attach(Description.FLOAT, Origin.REFLECTION);
        if (raw == Double.class || raw == double.class)
            return Description.attach(Description.DOUBLE, Origin.REFLECTION);
        if (raw == Boolean.class || raw == boolean.class)
            return Description.attach(Description.BOOLEAN, Origin.REFLECTION);
        if (raw == Void.class || raw == void.class)
            return Description.attach(Description.UNDEFINED, Origin.REFLECTION);
        if (raw == BigInteger.class)
            return Description.attach(Description.BIG_INTEGER, Origin.REFLECTION);
        if (raw == String.class)
            return Description.attach(Description.STRING, Origin.REFLECTION);
        if (raw == Object.class || raw == Value.class)
            return Description.attach(Description.ANY, Origin.REFLECTION);
        if (raw.isArray())
            return Description.attach(Description.concat(
                    Tau.describe(type instanceof GenericArrayType gat
                            ? gat.getGenericComponentType()
                            : raw.getComponentType()),
                    Description.delimiter("[]")
            ), Origin.REFLECTION);
        if (type instanceof ParameterizedType pt) {
            var generics = pt.getActualTypeArguments();
            return Description.attach(Description.concat(
                    Description.reference(raw),
                    Description.concat(
                            Description.delimiter('<'),
                            Description.join(Description.delimiter(", "), Arrays.stream(generics)
                                    .map(Tau::describe)
                                    .toArray(Description[]::new)),
                            Description.delimiter('>')
                    )
            ), Origin.REFLECTION);
        }
        return Description.attach(Description.reference(raw), Origin.REFLECTION);
    }

    private static @NotNull Class<?> raw(@NotNull Type type) {
        Objects.requireNonNull(type);
        return switch (type) {
            case Class<?> clazz -> clazz;
            case ParameterizedType pt -> Tau.raw(pt.getRawType());
            case TypeVariable<?> tv -> {
                var bounds = tv.getBounds();
                if (bounds.length == 1)
                    yield Tau.raw(bounds[0]);
                yield Object.class;
            }
            case WildcardType wt -> {
                var upper = wt.getUpperBounds();
                if (upper.length == 1)
                    yield Tau.raw(upper[0]);
                yield Object.class;
            }
            case GenericArrayType gat -> Array.newInstance(
                    Tau.raw(gat.getGenericComponentType()), 0
            ).getClass();
            default -> Object.class;
        };
    }

    @ApiStatus.Internal
    private static @Nullable Object loadUndefined() {
        try {
            var clazz = Class.forName("com.oracle.truffle.js.runtime.objects.Undefined");
            var instance = clazz.getDeclaredField("instance");
            if (instance.trySetAccessible())
                return instance.get(null);
            return null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
