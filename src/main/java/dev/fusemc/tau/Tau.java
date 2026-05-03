package dev.fusemc.tau;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import org.graalvm.polyglot.proxy.Proxy;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.*;

/// The Tau's entrypoint.
///
/// **Tau** (τ) is a runtime [Polyglot](https://www.graalvm.org/latest/reference-manual/polyglot-programming/) [Value] type-validation library, built
/// originally for [Fuse](https://fusemc.dev).
///
/// @since `0.1.0`
public final class Tau {

    @ApiStatus.Internal
    private static final @Nullable Object UNDEFINED_SENTINEL = Tau.loadUndefined();
    private static final int CONSTANT_LENGTH_THRESHOLD = 6;

    private Tau() {
        throw new UnsupportedOperationException();
    }
    
    /// Attempts to [Template#lower(org.graalvm.polyglot.Value)] the provided [Value] using the provided
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
    public static <T> T lower(@NotNull Template<T> template, @NotNull Value value) {
        Objects.requireNonNull(template);
        Objects.requireNonNull(value);
        var option = template.lower(value);
        if (option instanceof Option.Some<T>(var result))
            return result;
        throw new TypeException(Tau.describe(value), template.describe(Scope.hashScope()));
    }

    /// Attempts to [Template#raise(java.lang.Object)]  the provided [T] using the provided
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
    /// a "_nullable_" value.
    ///
    /// If the `undefined` sentinel couldn't be accessed at runtime,
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

    /// Describes the provided [Object].
    ///
    /// Produces a [Description] based on the given `Object` by inspecting its
    /// runtime value. Unless redirected to an overload, the produced
    /// `Description` will be annotated as having come from [Domain#HOST].
    ///
    /// If the provided `Object` is a [Value], a [Proxy] or a [Type], the more appropriate
    /// overload will be taken instead.
    ///
    /// @since `0.1.0`
    /// @see #describe(Value)
    /// @see #describe(Proxy)
    /// @see #describe(Type)
    public static @NotNull Description describe(@Nullable Object o) {
        return Tau.describe(o, Scope.hashScope(), true);
    }

    @ApiStatus.Internal
    private static @NotNull Description describe(@Nullable Object o,
                                                 @NotNull Scope<@NotNull Object> visited,
                                                 boolean constant) {
        if (o != null) {
            if (o instanceof Value value)
                return Tau.describe(value, visited, constant);
            if (o instanceof Proxy proxy)
                return Tau.describe(proxy, visited, constant);
            if (o instanceof Type type)
                return Tau.describe(type);
            if (o instanceof Number num) {
                if (constant)
                    return Description.attach(Description.number(num), Domain.HOST);
                return Description.attach(Description.NUMBER, Domain.HOST);
            }
            if (o instanceof String literal) {
                if (constant)
                    return Description.attach(Description.literal(literal), Domain.HOST);
                return Description.attach(Description.STRING, Domain.HOST);
            }
            if (o instanceof Boolean bl) {
                if (constant)
                    return bl ? Description.TRUE : Description.FALSE;
                return Description.attach(Description.BOOLEAN, Domain.HOST);
            }
            if (o instanceof byte[] bytes) {
                if (constant && bytes.length > 0 && bytes.length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[bytes.length];
                    for (var i = 0; i < bytes.length; i++)
                        buffer[i] = Tau.describe(bytes[i], visited.branch(), true);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Domain.HOST);
                }
                return Description.attach(Description.concat(
                        Description.BYTE,
                        Description.delimiter("[]")
                ), Domain.HOST);
            }
            if (o instanceof short[] shorts) {
                if (constant && shorts.length > 0 && shorts.length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[shorts.length];
                    for (var i = 0; i < shorts.length; i++)
                        buffer[i] = Tau.describe(shorts[i], visited.branch(), true);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Domain.HOST);
                }
                return Description.attach(Description.concat(
                        Description.SHORT,
                        Description.delimiter("[]")
                ), Domain.HOST);
            }
            if (o instanceof int[] ints) {
                if (constant && ints.length > 0 && ints.length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[ints.length];
                    for (var i = 0; i < ints.length; i++)
                        buffer[i] = Tau.describe(ints[i], visited.branch(), true);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Domain.HOST);
                }
                return Description.attach(Description.concat(
                        Description.INTEGER,
                        Description.delimiter("[]")
                ), Domain.HOST);
            }
            if (o instanceof long[] longs) {
                if (constant && longs.length > 0 && longs.length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[longs.length];
                    for (var i = 0; i < longs.length; i++)
                        buffer[i] = Tau.describe(longs[i], visited.branch(), true);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Domain.HOST);
                }
                return Description.attach(Description.concat(
                        Description.LONG,
                        Description.delimiter("[]")
                ), Domain.HOST);
            }
            if (o instanceof float[] floats) {
                if (constant && floats.length > 0 && floats.length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[floats.length];
                    for (var i = 0; i < floats.length; i++)
                        buffer[i] = Tau.describe(floats[i], visited.branch(), true);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Domain.HOST);
                }
                return Description.attach(Description.concat(
                        Description.FLOAT,
                        Description.delimiter("[]")
                ), Domain.HOST);
            }
            if (o instanceof double[] doubles) {
                if (constant && doubles.length > 0 && doubles.length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[doubles.length];
                    for (var i = 0; i < doubles.length; i++)
                        buffer[i] = Tau.describe(doubles[i], visited.branch(), true);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Domain.HOST);
                }
                return Description.attach(Description.concat(
                        Description.DOUBLE,
                        Description.delimiter("[]")
                ), Domain.HOST);
            }
            if (o instanceof boolean[] booleans) {
                if (constant && booleans.length > 0 && booleans.length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[booleans.length];
                    for (var i = 0; i < booleans.length; i++)
                        buffer[i] = Tau.describe(booleans[i], visited.branch(), true);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Domain.HOST);
                }
                return Description.attach(Description.concat(
                        Description.BOOLEAN,
                        Description.delimiter("[]")
                ), Domain.HOST);
            }
            if (o instanceof Object[] os) {
                if (visited.add(o)) {
                    if (os.length == 0)
                        return Description.attach(Description.concat(
                                Description.ANY,
                                Description.delimiter("[]")
                        ), Domain.HOST);
                    if (constant && os.length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                        var buffer = new Description[os.length];
                        for (var i = 0; i < os.length; i++)
                            buffer[i] = Tau.describe(os[i], visited.branch(), true);
                        return Description.attach(Description.concat(
                                Description.delimiter('['),
                                Description.join(
                                        Description.delimiter(", "),
                                        buffer
                                ),
                                Description.delimiter(']')
                        ), Domain.HOST);
                    }
                    return Description.attach(Description.concat(
                            Description.concat(
                                    Description.delimiter('('),
                                    Description.join(
                                            Description.delimiter(" | "),
                                            Arrays.stream(os)
                                                    .map(el -> Tau.describe(el, visited.branch(), false))
                                                    .distinct()
                                                    .toArray(Description[]::new)
                                    ),
                                    Description.delimiter(')')
                            ),
                            Description.delimiter("[]")
                    ), Domain.HOST);
                }
                return Description.attach(Description.ELLIPSIS, Domain.HOST);
            }
            if (o instanceof Map<?, ?> map) {
                if (visited.add(o)) {
                    var length = map.size();
                    if (length == 0)
                        return Description.attach(Description.delimiter("{}"), Domain.HOST);
                    if (constant && length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                        var buffer = new Description[length];
                        var i = 0;
                        for (var entry : map.entrySet()) {
                            var key = entry.getKey();
                            var value = entry.getValue();
                            buffer[i++] = Description.concat(
                                     Description.concat(
                                            Description.delimiter('['),
                                            Tau.describe(key, visited.branch(), true),
                                            Description.delimiter(']')
                                    ),
                                    Description.delimiter(": "),
                                    Tau.describe(value, visited.branch(), true)
                            );
                        }
                        return Description.attach(Description.concat(
                                Description.delimiter('{'),
                                Description.join(Description.delimiter(", "), buffer),
                                Description.delimiter('}')
                        ), Domain.HOST);
                    }
                    return Description.attach(Description.concat(
                            Description.delimiter('{'),
                            Description.concat(
                                    Description.delimiter('['),
                                    Description.join(Description.delimiter(" | "), map.keySet()
                                            .stream()
                                            .map(key -> Tau.describe(key, visited.branch(), false))
                                            .distinct()
                                            .toArray(Description[]::new)),
                                    Description.delimiter(']')
                            ),
                            Description.delimiter(": "),
                            Description.join(Description.delimiter(" | "),  map.values()
                                    .stream()
                                    .map(value -> Tau.describe(value, visited.branch(), false))
                                    .distinct()
                                    .toArray(Description[]::new)),
                            Description.delimiter('}')
                    ), Domain.HOST);
                }
                return Description.attach(Description.ELLIPSIS, Domain.HOST);
            }
            return Description.attach(Description.reference(o.getClass()), Domain.HOST);
        }
        return Description.attach(Description.NULL, Domain.HOST);
    }

    /// Describes the provided [Value].
    ///
    /// Produces a [Description] based on the given `Value`.
    /// The produced `Description` will be annotated as having come from [Domain#POLYGLOT].
    ///
    /// If the provided `Value` wraps a **Host Object**, the [#describe(Object)] overload
    /// will be taken to describe it. The description will, however, still be
    /// annotated as [Domain#POLYGLOT].
    ///
    /// @since `0.1.0`
    /// @see #describe(Proxy)
    /// @see #describe(Type)
    public static @NotNull Description describe(@NotNull Value value) {
        Objects.requireNonNull(value);
        return Tau.describe(value, Scope.hashScope(), true);
    }

    @ApiStatus.Internal
    private static @NotNull Description describe(@NotNull Value value,
                                                 @NotNull Scope<@NotNull Object> visited,
                                                 boolean constant) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(visited);
        if (value.isNumber()) {
            if (constant) {
                if (value.fitsInByte())
                    return Description.attach(Description.number(value.asByte()), Domain.POLYGLOT);
                if (value.fitsInShort())
                    return Description.attach(Description.number(value.asShort()), Domain.POLYGLOT);
                if (value.fitsInInt())
                    return Description.attach(Description.number(value.asInt()), Domain.POLYGLOT);
                if (value.fitsInLong())
                    return Description.attach(Description.number(value.asLong()), Domain.POLYGLOT);
                if (value.fitsInBigInteger())
                    return Description.attach(Description.number(value.asBigInteger()), Domain.POLYGLOT);
                if (value.fitsInFloat())
                    return Description.attach(Description.number(value.asFloat()), Domain.POLYGLOT);
                if (value.fitsInDouble())
                    return Description.attach(Description.number(value.asDouble()), Domain.POLYGLOT);
                return Description.attach(Description.NUMBER, Domain.POLYGLOT);
            }
            return Description.attach(Description.NUMBER, Domain.POLYGLOT);
        }
        if (value.isString()) {
            if (constant)
                return Description.attach(Description.literal(value.asString()), Domain.POLYGLOT);
            return Description.attach(Description.STRING, Domain.POLYGLOT);
        }
        if (value.isBoolean()) {
            if (constant)
                return value.asBoolean() ? Description.TRUE : Description.FALSE;
            return Description.attach(Description.BOOLEAN, Domain.POLYGLOT);
        }
        if (Tau.isUndefined(value))
            return Description.attach(Description.UNDEFINED, Domain.POLYGLOT);
        if (Tau.isNull(value))
            return Description.attach(Description.NULL, Domain.POLYGLOT);
        if (value.hasArrayElements()) {
            if (visited.add(value)) {
                var length = (int) value.getArraySize();
                if (length == 0)
                    return Description.attach(Description.concat(
                            Description.ANY,
                            Description.delimiter("[]")
                    ), Domain.POLYGLOT);
                if (constant && length < Tau.CONSTANT_LENGTH_THRESHOLD) {
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
                    ), Domain.POLYGLOT);
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
                ), Domain.POLYGLOT);
            }
            return Description.attach(Description.ELLIPSIS, Domain.POLYGLOT);
        }
        if (value.hasHashEntries()) {
            if (visited.add(value)) {
                var length = (int) value.getHashSize();
                if (length ==  0)
                    return Description.attach(Description.delimiter("{}"), Domain.POLYGLOT);
                var iterator = Tau.lower(
                        Template.iterator(Template.<Value[], Value, Value>tuple(
                                Template.element(Template.ANY, values -> values[0]),
                                Template.element(Template.ANY, values -> values[1]),
                                (a, b) -> new Value[] {a, b}
                        )),
                        value.getHashEntriesIterator()
                );
                if (constant && length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[length];
                    for (var i = 0; iterator.hasNext(); i++) {
                        var entry = iterator.next();
                        buffer[i] = Description.concat(
                                Description.concat(
                                        Description.delimiter('['),
                                        Tau.describe(entry[0], visited.branch(), true),
                                        Description.delimiter(']')
                                ),
                                Description.delimiter(": "),
                                Tau.describe(entry[1], visited.branch(), true)
                        );
                    }
                    return Description.attach(Description.concat(
                            Description.delimiter('{'),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter('}')
                    ), Domain.POLYGLOT);
                }
                var keys = new LinkedHashSet<Description>();
                var values = new LinkedHashSet<Description>();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    keys.add(Tau.describe(entry[0], visited.branch(), false));
                    values.add(Tau.describe(entry[1], visited.branch(), false));
                }
                return Description.attach(Description.concat(
                        Description.delimiter('{'),
                        Description.concat(
                                Description.delimiter('['),
                                Description.join(
                                        Description.delimiter(" | "),
                                        keys.toArray(Description[]::new)
                                ),
                                Description.delimiter(']')
                        ),
                        Description.delimiter(": "),
                        Description.join(
                                Description.delimiter(" | "),
                                values.toArray(Description[]::new)
                        ),
                        Description.delimiter('}')
                ), Domain.POLYGLOT);
            }
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
                ), Domain.POLYGLOT);
            }
            return Description.attach(Description.ELLIPSIS, Domain.POLYGLOT);
        }
        if (value.isHostObject())
            return Tau.describe((Object) value.asHostObject(), visited, constant);
        if (value.isProxyObject())
            return Tau.describe((Proxy) value.asProxyObject(), visited, constant);
        return Description.attach(Description.UNKNOWN, Domain.POLYGLOT);
    }

    /// Describes the provided [Proxy].
    ///
    /// Produces a [Description] based on the given `Proxy`.
    /// The produced `Description` will be annotated as having come from [Domain#PROXY].
    ///
    /// @since `0.1.0`
    /// @see #describe(Value)
    /// @see #describe(Type)
    public static @NotNull Description describe(@NotNull Proxy proxy) {
        Objects.requireNonNull(proxy);
        return Tau.describe(proxy, Scope.hashScope(), true);
    }

    @ApiStatus.Internal
    private static @NotNull Description describe(@NotNull Proxy proxy,
                                                 @NotNull Scope<@NotNull Object> visited,
                                                 boolean constant) {
        Objects.requireNonNull(proxy);
        Objects.requireNonNull(visited);
        if (proxy instanceof ProxyArray array) {
            if (visited.add(array)) {
                var length = (int) array.getSize();
                if (length == 0)
                    return Description.attach(Description.concat(
                            Description.ANY,
                            Description.delimiter("[]")
                    ), Domain.PROXY);
                if (constant && length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[length];
                    for (var i = 0; i < length; i++)
                        buffer[i] = Tau.describe(array.get(i), visited.branch(), true);
                    return Description.attach(Description.concat(
                            Description.delimiter('['),
                            Description.join(
                                    Description.delimiter(", "),
                                    buffer
                            ),
                            Description.delimiter(']')
                    ), Domain.PROXY);
                }
                var buffer = new LinkedHashSet<Description>();
                for (var i = 0; i < length; i++)
                    buffer.add(Tau.describe(array.get(i), visited.branch(), false));
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
                ), Domain.PROXY);
            }
            return Description.attach(Description.ELLIPSIS, Domain.PROXY);
        }
        if (proxy instanceof ProxyObject object) {
            if (visited.add(object)) {
                var keys = Tau.lower(
                        Template.array(Template.STRING, String[]::new),
                        Value.asValue(object.getMemberKeys())
                );
                var buffer = new Description[keys.length];
                for (var i = 0; i < keys.length; i++) {
                    var member = object.getMember(keys[i]);
                    var description = Tau.describe(member, visited.branch(), constant);
                    buffer[i] = Description.concat(
                            Description.literal(keys[i]),
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
                ), Domain.PROXY);
            }
            return Description.attach(Description.ELLIPSIS, Domain.PROXY);
        }
        if (proxy instanceof ProxyHashMap map) {
            if (visited.add(map)) {
                var length = (int) map.getHashSize();
                var iterator = Tau.lower(
                        Template.iterator(Template.<Value[], Value, Value>tuple(
                                Template.element(Template.ANY, tuple -> tuple[0]),
                                Template.element(Template.ANY, tuple -> tuple[1]),
                                (a, b) -> new Value[] {a, b}
                        )),
                        Value.asValue(map.getHashEntriesIterator())
                );
                if (length == 0)
                    return Description.attach(Description.delimiter("{}"), Domain.PROXY);
                if (constant && length < Tau.CONSTANT_LENGTH_THRESHOLD) {
                    var buffer = new Description[length];
                    for (var i = 0; iterator.hasNext(); ) {
                        var entry = iterator.next();
                        buffer[i++] = Description.concat(
                                Description.concat(
                                        Description.delimiter('['),
                                        Tau.describe(entry[0], visited.branch(), true),
                                        Description.delimiter(']')
                                ),
                                Description.delimiter(": "),
                                Tau.describe(entry[1], visited.branch(), true)
                        );
                    }
                    return Description.attach(Description.concat(
                            Description.delimiter('{'),
                            Description.join(Description.delimiter(", "), buffer),
                            Description.delimiter('}')
                    ), Domain.PROXY);
                }
                var key = new LinkedHashSet<Description>();
                var value = new LinkedHashSet<Description>();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    key.add(Tau.describe(entry[0], visited.branch(), false));
                    value.add(Tau.describe(entry[1], visited.branch(), false));
                }
                return Description.attach(Description.concat(
                        Description.delimiter('{'),
                        Description.concat(
                                Description.delimiter('['),
                                Description.join(
                                        Description.delimiter(" | "),
                                        key.toArray(Description[]::new)
                                ),
                                Description.delimiter(']')
                        ),
                        Description.delimiter(": "),
                        Description.join(
                                Description.delimiter(" | "),
                                value.toArray(Description[]::new)
                        ),
                        Description.delimiter('}')
                ), Domain.PROXY);
            }
            return Description.attach(Description.ELLIPSIS, Domain.PROXY);
        }
        return Description.attach(Description.UNKNOWN, Domain.PROXY);
    }

    /// Describes the provided reflected [Type].
    ///
    /// Produces a [Description] based on the given `Type`.
    /// The produced description will be annotated as having come from [Domain#REFLECTION].
    ///
    /// Any generic metadata on the given `Type` will be preserved.
    ///
    /// @since `0.1.0`
    /// @see #describe(Object)
    /// @see #describe(Value)
    public static @NotNull Description describe(@NotNull Type type) {
        Objects.requireNonNull(type);
        var raw = Tau.raw(type);
        if (raw == Byte.class    || raw == byte.class)
            return Description.attach(Description.BYTE,        Domain.REFLECTION);
        if (raw == Short.class   || raw == short.class)
            return Description.attach(Description.SHORT,       Domain.REFLECTION);
        if (raw == Integer.class || raw == int.class)
            return Description.attach(Description.INTEGER,     Domain.REFLECTION);
        if (raw == Long.class    || raw == long.class)
            return Description.attach(Description.LONG,        Domain.REFLECTION);
        if (raw == Float.class   || raw == float.class)
            return Description.attach(Description.FLOAT,       Domain.REFLECTION);
        if (raw == Double.class  || raw == double.class)
            return Description.attach(Description.DOUBLE,      Domain.REFLECTION);
        if (raw == Boolean.class || raw == boolean.class)
            return Description.attach(Description.BOOLEAN,     Domain.REFLECTION);
        if (raw == Void.class    || raw == void.class)
            return Description.attach(Description.UNDEFINED,   Domain.REFLECTION);
        if (raw == Object.class  || raw == Value.class)
            return Description.attach(Description.ANY,         Domain.REFLECTION);
        if (raw == BigInteger.class)
            return Description.attach(Description.BIG_INTEGER, Domain.REFLECTION);
        if (raw == String.class)
            return Description.attach(Description.STRING,      Domain.REFLECTION);
        if (raw.isArray())
            return Description.attach(Description.concat(
                    Tau.describe(type instanceof GenericArrayType gat
                            ? gat.getGenericComponentType()
                            : raw.getComponentType()),
                    Description.delimiter("[]")
            ), Domain.REFLECTION);
        if (type instanceof ParameterizedType pt)
            return Description.attach(Description.concat(
                    Description.reference(raw),
                    Description.concat(
                            Description.delimiter('<'),
                            Description.join(
                                    Description.delimiter(", "),
                                    Arrays.stream(pt.getActualTypeArguments())
                                        .map(Tau::describe)
                                        .toArray(Description[]::new)
                            ),
                            Description.delimiter('>')
                    )
            ), Domain.REFLECTION);
        return Description.attach(Description.reference(raw), Domain.REFLECTION);
    }

    @ApiStatus.Internal
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
