package dev.fusemc.tau;

import com.manchickas.optionated.Option;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.regex.RegexObject;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import org.graalvm.polyglot.proxy.Proxy;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

/// The Tau's entrypoint.
///
/// **Tau** (τ) is a runtime [Polyglot](https://www.graalvm.org/latest/reference-manual/polyglot-programming/) [Value] type-validation library, built
/// originally for [Fuse](https://fusemc.dev).
///
/// @since `0.1.0`
public final class Tau {

    private static final @Nullable Object UNDEFINED_SENTINEL = Tau.loadUndefined();
    private static final @NotNull VarHandle RECEIVER = Tau.receiver();
    private static final @NotNull Pattern IDENTIFIER = Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");
    private static final @NotNull Description PROTOTYPE = Description.concat(
            Description.delimiter('['),
            Description.concat(
                    Description.delimiter("object"),
                    Description.delimiter(' '),
                    Description.reference("Object")
            ),
            Description.delimiter(']')
    );
    private static final @NotNull Description FUNCTION = Description.concat(
            Description.delimiter('['),
            Description.concat(
                    Description.delimiter("object"),
                    Description.delimiter(' '),
                    Description.reference("Function")
            ),
            Description.delimiter(']')
    );
    private static final int LENGTH_THRESHOLD = 5;

    private Tau() {
        throw new UnsupportedOperationException();
    }

    /// Attempt to lower the provided [Value] as a **RegExp** [Pattern].
    ///
    /// ---
    ///
    /// Due to GraalVM not having a supported way of working with regular expressions from Java,
    /// it is unreasonably difficult to raise a `String` to a [JSRegExpObject]. It is, however,
    /// relatively straightforward to lower a RegExp [Value] to a respective [Pattern]. This method
    /// thus acts as a replacement for a proper RegExp-[Template].
    ///
    /// If the provided [Value] is not a RegExp, a [TypeException] will be thrown in the form of:
    ///
    /// ```
    /// Type '...' is not assignable to type 'RegExp'.
    /// ```
    ///
    /// @since `0.2.9`
    @SuppressWarnings("MagicConstant")
    public static @NotNull Pattern lowerPattern(@NotNull Value value) {
        Objects.requireNonNull(value);
        var receiver = Tau.RECEIVER.get(value);
        if (receiver instanceof JSRegExpObject jsr) {
            var compiled = jsr.getCompiledRegex();
            if (compiled instanceof RegexObject regex) {
                var source = regex.getSource();
                var flags  = source.getFlags()
                        .codePoints()
                        .map(flag -> switch (flag) {
                            case 'm' -> Pattern.MULTILINE;
                            case 'i' -> Pattern.CASE_INSENSITIVE;
                            case 'u' -> Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;
                            case 's' -> Pattern.DOTALL;
                            default -> 0;
                        })
                        .reduce(0, (a, b) -> a | b);
                return Pattern.compile(source.getPattern(), flags);
            }
            throw new TypeException(Tau.describe(value), Description.reference("RegExp"));
        }
        throw new TypeException(Tau.describe(value), Description.reference("RegExp"));
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

    /// Attempt to [Template#raise(java.lang.Object)] the provided [T] using the provided
    /// [Template].
    ///
    /// ---
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

    /// Return an [`undefined`](https://tc39.es/ecma262/#sec-ecmascript-language-types-undefined-type) [Value].
    ///
    /// ---
    /// If the `undefined` sentinel couldn't be accessed at runtime,
    /// the method degrades to returning `Value.asValue(null)`.
    ///
    /// @since `0.1.0`
    /// @see #isUndefined(Value)
    public static @NotNull Value undefined() {
        return Value.asValue(Tau.UNDEFINED_SENTINEL);
    }

    /// Determine whether the provided [Value] is explicitly [`undefined`](https://tc39.es/ecma262/#sec-ecmascript-language-types-undefined-type).
    ///
    /// ---
    /// If the `undefined` sentinel couldn't be accessed at runtime,
    /// the method degrades to always returning `false`.
    ///
    /// @since `0.1.0`
    /// @see #isNull(Value)
    /// @see #undefined()
    public static boolean isUndefined(@NotNull Value value) {
        Objects.requireNonNull(value);
        if (Tau.UNDEFINED_SENTINEL != null)
            return value.hashCode() == Tau.UNDEFINED_SENTINEL.hashCode();
        return false;
    }

    /// Determine whether the provided `Value` is explicitly `null`.
    ///
    /// ---
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

    /// Return the **length threshold**.
    ///
    /// ---
    /// The 'length threshold' is the largest a structure can get before
    /// its description is collapsed into a narrow form. It corresponds
    /// to the biggest arity of the `Template.record()` and `Template.tuple()`
    /// methods.
    ///
    /// @since `0.2.4`
    public static int lengthThreshold() {
        return Tau.LENGTH_THRESHOLD;
    }

    public static @NotNull Description inspect(@Nullable Object object,
                                               @NotNull Scope<Object> visited) {
        Objects.requireNonNull(visited);
        if (object == null)
            return Description.NULL;
        if (object instanceof String string)
            return Description.literal(string);
        if (object instanceof Number number)
            return Description.numeric(number);
        if (object instanceof Boolean bool)
            return bool ? Description.TRUE : Description.FALSE;
        if (object instanceof byte[] bytes) {
            var buffer = new Description[Math.min(bytes.length, 100)];
            for (var i = 0; i < bytes.length; i++) {
                if (i < 99 || bytes.length == 100) {
                    buffer[i] = Description.numeric(bytes[i]);
                    continue;
                }
                buffer[i] = Tau.nMore(bytes.length - i);
                break;
            }
            return Description.join(Description.delimiter(", "), buffer);
        }
        if (object instanceof short[] shorts) {
            var buffer = new Description[Math.min(shorts.length, 100)];
            for (var i = 0; i < shorts.length; i++) {
                if (i < 99 || shorts.length == 100) {
                    buffer[i] = Description.numeric(shorts[i]);
                    continue;
                }
                buffer[i] = Tau.nMore(shorts.length - i);
                break;
            }
            return Description.join(Description.delimiter(", "), buffer);
        }
        if (object instanceof int[] ints) {
            var buffer = new Description[Math.min(ints.length, 100)];
            for (var i = 0; i < ints.length; i++) {
                if (i < 99 || ints.length == 100) {
                    buffer[i] = Description.numeric(ints[i]);
                    continue;
                }
                buffer[i] = Tau.nMore(ints.length - i);
                break;
            }
            return Description.join(Description.delimiter(", "), buffer);
        }
        if (object instanceof long[] longs) {
            var buffer = new Description[Math.min(longs.length, 100)];
            for (var i = 0; i < longs.length; i++) {
                if (i < 99 || longs.length == 100) {
                    buffer[i] = Description.numeric(longs[i]);
                    continue;
                }
                buffer[i] = Tau.nMore(longs.length - i);
                break;
            }
            return Description.join(Description.delimiter(", "), buffer);
        }
        if (object instanceof float[] floats) {
            var buffer = new Description[Math.min(floats.length, 100)];
            for (var i = 0; i < floats.length; i++) {
                if (i < 99 || floats.length == 100) {
                    buffer[i] = Description.numeric(floats[i]);
                    continue;
                }
                buffer[i] = Tau.nMore(floats.length - i);
                break;
            }
            return Description.join(Description.delimiter(", "), buffer);
        }
        if (object instanceof double[] doubles) {
            var buffer = new Description[Math.min(doubles.length, 100)];
            for (var i = 0; i < doubles.length; i++) {
                if (i < 99 || doubles.length == 100) {
                    buffer[i] = Description.numeric(doubles[i]);
                    continue;
                }
                buffer[i] = Tau.nMore(doubles.length - i);
                break;
            }
            return Description.join(Description.delimiter(", "), buffer);
        }
        if (object instanceof boolean[] booleans) {
            var buffer = new Description[Math.min(booleans.length, 100)];
            for (var i = 0; i < booleans.length; i++) {
                if (i < 99 || booleans.length == 100) {
                    buffer[i] = booleans[i] ? Description.TRUE : Description.FALSE;
                    continue;
                }
                buffer[i] = Tau.nMore(booleans.length - i);
                break;
            }
            return Description.join(Description.delimiter(", "), buffer);
        }
        if (object instanceof Object[] objects) {
            if (visited.add(object)) {
                var buffer = new Description[Math.min(objects.length, 100)];
                for (var i = 0; i < objects.length; i++) {
                    if (i < 99 || objects.length == 100) {
                        buffer[i] = Tau.inspect(objects[i], visited.branch());
                        continue;
                    }
                    buffer[i] = Tau.nMore(objects.length - i);
                    break;
                }
                return Description.join(Description.delimiter(", "), buffer);
            }
            return Description.ELLIPSIS;
        }
        if (object instanceof Map<?, ?> map) {
            if (visited.add(object)) {
                var length = map.size();
                var buffer = new Description[Math.min(length, 100)];
                var iterator = map.entrySet().iterator();
                for (var i = 0; iterator.hasNext(); i++) {
                    var entry = iterator.next();
                    var key   = entry.getKey();
                    var value = entry.getValue();
                    if (i < 99 || length == 100) {
                        buffer[i] = Description.concat(
                                Description.concat(
                                        Description.delimiter('['),
                                        Tau.inspect(key, visited.branch()),
                                        Description.delimiter(']')
                                ),
                                Description.delimiter(": "),
                                Tau.inspect(value, visited.branch())
                        );
                        continue;
                    }
                    buffer[i] = Tau.nMore(length - i);
                    break;
                }
                return Description.concat(
                        Description.delimiter('{'),
                        Description.join(Description.delimiter(", "), buffer),
                        Description.delimiter('}')
                );
            }
            return Description.ELLIPSIS;
        }
        if (object instanceof Inspectable inspectable)
            return inspectable.inspect();
        if (object instanceof Value value)
            return Tau.inspect(value, visited);
        if (object instanceof Proxy proxy)
            return Tau.inspect(proxy, visited);
        return Tau.PROTOTYPE;
    }

    public static @NotNull Description inspect(@NotNull Value value, @NotNull Scope<Object> visited) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(visited);
        if (Tau.isUndefined(value))
            return Description.UNDEFINED;
        if (Tau.isNull(value))
            return Description.NULL;
        if (value.isNumber()) {
            if (value.fitsInByte())
                return Description.numeric(value.asByte());
            if (value.fitsInShort())
                return Description.numeric(value.asShort());
            if (value.fitsInInt())
                return Description.numeric(value.asInt());
            if (value.fitsInLong())
                return Description.numeric(value.asLong());
            if (value.fitsInBigInteger())
                return Description.numeric(value.asBigInteger());
            if (value.fitsInFloat())
                return Description.numeric(value.asFloat());
            return Description.numeric(value.asDouble());
        }
        if (value.isBoolean())
            return value.asBoolean() ? Description.TRUE : Description.FALSE;
        if (value.isString())
            return Description.literal(value.asString());
        if (value.isProxyObject())
            return Tau.inspect((Proxy) value.asProxyObject(), visited);
        if (value.isHostObject())
            return Tau.inspect((Object) value.asHostObject(), visited);
        if (value.hasArrayElements()) {
            if (visited.add(value)) {
                var length = (int) value.getArraySize();
                var buffer = new Description[Math.min(length, 100)];
                for (var i = 0; i < length; i++) {
                    if (i < 99 || length == 100) {
                        buffer[i] = Tau.inspect(value.getArrayElement(i), visited.branch());
                        continue;
                    }
                    buffer[i] = Tau.nMore(length - i);
                    break;
                }
                return Description.concat(
                        Description.delimiter('['),
                        Description.join(Description.delimiter(", "), buffer),
                        Description.delimiter(']')
                );
            }
            return Description.ELLIPSIS;
        }
        if (value.hasHashEntries()) {
            if (visited.add(value)) {
                var length   = (int) value.getHashSize();
                var buffer   = new Description[Math.min(length, 100)];
                var iterator = Tau.lower(
                        Template.iterator(Template.<Value[], Value, Value>tuple(
                                Template.ANY.element(values -> values[0]),
                                Template.ANY.element(values -> values[1]),
                                (a, b) -> new Value[] {a, b}
                        )),
                        value.getHashEntriesIterator()
                );
                for (var i = 0; iterator.hasNext(); i++) {
                    var entry = iterator.next();
                    if (i < 99 || length == 100) {
                        buffer[i] = Description.concat(
                                Description.concat(
                                        Description.delimiter('['),
                                        Tau.inspect(entry[0], visited.branch()),
                                        Description.delimiter(']')
                                ),
                                Description.delimiter(": "),
                                Tau.inspect(entry[1], visited.branch())
                        );
                        continue;
                    }
                    buffer[i] = Tau.nMore(length - i);
                    break;
                }
                return Description.concat(
                        Description.delimiter('{'),
                        Description.join(Description.delimiter(", "), buffer),
                        Description.delimiter('}')
                );
            }
            return Description.ELLIPSIS;
        }
        if (value.hasMembers()) {
            if (visited.add(value)) {
                var keys     = value.getMemberKeys();
                var iterator = keys.iterator();
                var length   = keys.size();
                var buffer   = new Description[Math.min(length, 100)];
                for (var i = 0; iterator.hasNext(); i++) {
                    var key = iterator.next();
                    if (i < 99 || length == 100) {
                        var matcher = Tau.IDENTIFIER.matcher(key);
                        buffer[i] = Description.concat(
                                matcher.matches()
                                        ? Description.delimiter(key)
                                        : Description.literal(key),
                                Description.delimiter(": "),
                                Tau.inspect(value.getMember(key), visited.branch())
                        );
                        continue;
                    }
                    buffer[i] = Tau.nMore(length - i);
                    break;
                }
                return Description.concat(
                        Description.delimiter('{'),
                        Description.join(Description.delimiter(", "), buffer),
                        Description.delimiter('}')
                );
            }
            return Description.ELLIPSIS;
        }
        if (value.canExecute())
            return Tau.FUNCTION;
        return Tau.PROTOTYPE;
    }

    public static @NotNull Description inspect(@NotNull Proxy proxy, @NotNull Scope<Object> visited) {
        if (proxy instanceof Inspectable inspectable)
            return inspectable.inspect();
        if (proxy instanceof ProxyArray array) {
            if (visited.add(proxy)) {
                var length = (int) array.getSize();
                var buffer = new Description[Math.min(length, 100)];
                for (var i = 0; i < length; i++) {
                    if (i < 99 || length == 100) {
                        buffer[i] = Tau.inspect(array.get(i), visited.branch());
                        continue;
                    }
                    buffer[i] = Tau.nMore(length - i);
                    break;
                }
                return Description.concat(
                        Description.delimiter('['),
                        Description.join(Description.delimiter(", "), buffer),
                        Description.delimiter(']')
                );
            }
            return Description.ELLIPSIS;
        }
        if (proxy instanceof ProxyObject object) {
            if (visited.add(proxy)) {
                var keys = Tau.lower(
                        Template.array(Template.STRING, String[]::new),
                        Value.asValue(object.getMemberKeys())
                );
                var buffer = new Description[Math.min(keys.length, 100)];
                for (var i = 0; i < keys.length; i++) {
                    var key = keys[i];
                    if (i < 99 || keys.length == 100) {
                        var matcher = Tau.IDENTIFIER.matcher(key);
                        buffer[i] = Description.concat(
                                matcher.matches()
                                        ? Description.delimiter(key)
                                        : Description.literal(key),
                                Description.delimiter(": "),
                                Tau.inspect(object.getMember(key), visited.branch())
                        );
                        continue;
                    }
                    buffer[i] = Tau.nMore(keys.length - i);
                    break;
                }
                return Description.concat(
                        Description.delimiter('{'),
                        Description.join(Description.delimiter(", "), buffer),
                        Description.delimiter('}')
                );
            }
            return Description.ELLIPSIS;
        }
        if (proxy instanceof ProxyHashMap map) {
            if (visited.add(proxy)) {
                var length  = (int) map.getHashSize();
                var buffer  = new Description[Math.min(length, 100)];
                var entries = Tau.lower(
                        Template.iterator(Template.<Value[], Value, Value>tuple(
                                Template.ANY.element(tuple -> tuple[0]),
                                Template.ANY.element(tuple -> tuple[1]),
                                (a, b) -> new Value[] {a, b}
                        )),
                        Value.asValue(map.getHashEntriesIterator())
                );
                for (var i = 0; entries.hasNext(); i++) {
                    var entry = entries.next();
                    if (i < 99 || length == 100) {
                        buffer[i] = Description.concat(
                                Description.concat(
                                        Description.delimiter('['),
                                        Tau.inspect(entry[0], visited.branch()),
                                        Description.delimiter(']')
                                ),
                                Description.delimiter(": "),
                                Tau.inspect(entry[1], visited.branch())
                        );
                        continue;
                    }
                    buffer[i] = Tau.nMore(length - i);
                    break;
                }
                return Description.concat(
                        Description.delimiter('{'),
                        Description.join(Description.delimiter(", "), buffer),
                        Description.delimiter('}')
                );
            }
            return Description.ELLIPSIS;
        }
        if (proxy instanceof ProxyExecutable)
            return Tau.FUNCTION;
        return Tau.PROTOTYPE;
    }

    @ApiStatus.Internal
    private static @NotNull Description nMore(int n) {
        return Description.concat(
                Description.delimiter("..."),
                Description.delimiter(Integer.toString(n)),
                Description.delimiter(" more")
        );
    }

    /// Describe the provided [Object].
    ///
    /// ---
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
                    return Description.attach(Description.numeric(num), Domain.HOST);
                return Description.attach(Description.NUMBER, Domain.HOST);
            }
            if (o instanceof String literal) {
                if (constant)
                    return Description.attach(Description.literal(literal), Domain.HOST);
                return Description.attach(Description.STRING, Domain.HOST);
            }
            if (o instanceof Boolean bl) {
                if (constant)
                    return Description.attach(bl ? Description.TRUE : Description.FALSE, Domain.HOST);
                return Description.attach(Description.BOOLEAN, Domain.HOST);
            }
            if (o instanceof byte[] bytes) {
                if (constant && bytes.length > 0 && bytes.length <= Tau.LENGTH_THRESHOLD) {
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
                if (constant && shorts.length > 0 && shorts.length <= Tau.LENGTH_THRESHOLD) {
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
                if (constant && ints.length > 0 && ints.length <= Tau.LENGTH_THRESHOLD) {
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
                if (constant && longs.length > 0 && longs.length <= Tau.LENGTH_THRESHOLD) {
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
                if (constant && floats.length > 0 && floats.length <= Tau.LENGTH_THRESHOLD) {
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
                if (constant && doubles.length > 0 && doubles.length <= Tau.LENGTH_THRESHOLD) {
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
                if (constant && booleans.length > 0 && booleans.length <= Tau.LENGTH_THRESHOLD) {
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
                    if (constant && os.length <= Tau.LENGTH_THRESHOLD) {
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
                    if (constant && length <= Tau.LENGTH_THRESHOLD) {
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
        if (Tau.isUndefined(value))
            return Description.attach(Description.UNDEFINED, Domain.POLYGLOT);
        if (Tau.isNull(value))
            return Description.attach(Description.NULL, Domain.POLYGLOT);
        if (value.isNumber()) {
            if (constant) {
                if (value.fitsInByte())
                    return Description.attach(Description.numeric(value.asByte()), Domain.POLYGLOT);
                if (value.fitsInShort())
                    return Description.attach(Description.numeric(value.asShort()), Domain.POLYGLOT);
                if (value.fitsInInt())
                    return Description.attach(Description.numeric(value.asInt()), Domain.POLYGLOT);
                if (value.fitsInLong())
                    return Description.attach(Description.numeric(value.asLong()), Domain.POLYGLOT);
                if (value.fitsInBigInteger())
                    return Description.attach(Description.numeric(value.asBigInteger()), Domain.POLYGLOT);
                if (value.fitsInFloat())
                    return Description.attach(Description.numeric(value.asFloat()), Domain.POLYGLOT);
                if (value.fitsInDouble())
                    return Description.attach(Description.numeric(value.asDouble()), Domain.POLYGLOT);
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
                return Description.attach(value.asBoolean() ? Description.TRUE : Description.FALSE, Domain.POLYGLOT);
            return Description.attach(Description.BOOLEAN, Domain.POLYGLOT);
        }
        if (value.isHostObject())
            return Tau.describe((Object) value.asHostObject(), visited, constant);
        if (value.isProxyObject())
            return Tau.describe((Proxy) value.asProxyObject(), visited, constant);
        if (value.hasArrayElements()) {
            if (visited.add(value)) {
                var length = (int) value.getArraySize();
                if (length == 0)
                    return Description.attach(Description.concat(
                            Description.ANY,
                            Description.delimiter("[]")
                    ), Domain.POLYGLOT);
                if (constant && length <= Tau.LENGTH_THRESHOLD) {
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
                                Template.ANY.element(values -> values[0]),
                                Template.ANY.element(values -> values[1]),
                                (a, b) -> new Value[] {a, b}
                        )),
                        value.getHashEntriesIterator()
                );
                if (constant && length <= Tau.LENGTH_THRESHOLD) {
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
                var iterator = keys.iterator();
                var buffer = new Description[keys.size()];
                for (var i = 0; iterator.hasNext(); i++) {
                    var key = iterator.next();
                    var matcher = Tau.IDENTIFIER.matcher(key);
                    buffer[i] = Description.concat(
                            matcher.matches()
                                    ? Description.delimiter(key)
                                    : Description.literal(key),
                            Description.delimiter(": "),
                            Tau.describe(value.getMember(key), visited.branch(), constant)
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
        var type = proxy.getClass();
        if (type.isAnnotationPresent(Documented.class))
            return Description.attach(Description.reference(type), Domain.HOST);
        if (proxy instanceof ProxyArray array) {
            if (visited.add(array)) {
                var length = (int) array.getSize();
                if (length == 0)
                    return Description.attach(Description.concat(
                            Description.ANY,
                            Description.delimiter("[]")
                    ), Domain.PROXY);
                if (constant && length <= Tau.LENGTH_THRESHOLD) {
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
                    var key = keys[i];
                    var matcher = Tau.IDENTIFIER.matcher(key);
                    buffer[i] = Description.concat(
                            matcher.matches()
                                ? Description.delimiter(key)
                                : Description.literal(key),
                            Description.delimiter(": "),
                            Tau.describe(object.getMember(key), visited.branch(), constant)
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
                                Template.ANY.element(tuple -> tuple[0]),
                                Template.ANY.element(tuple -> tuple[1]),
                                (a, b) -> new Value[] {a, b}
                        )),
                        Value.asValue(map.getHashEntriesIterator())
                );
                if (length == 0)
                    return Description.attach(Description.delimiter("{}"), Domain.PROXY);
                if (constant && length <= Tau.LENGTH_THRESHOLD) {
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
        return Description.attach(Description.reference(proxy.getClass()), Domain.HOST);
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
            var clazz    = Class.forName("com.oracle.truffle.js.runtime.objects.Undefined");
            var instance = clazz.getDeclaredField("instance");
            if (instance.trySetAccessible())
                return instance.get(null);
            return null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static @NotNull VarHandle receiver() {
        try {
            var lookup = MethodHandles.privateLookupIn(Value.class, MethodHandles.lookup());
            return lookup.findVarHandle(Value.class, "receiver", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
