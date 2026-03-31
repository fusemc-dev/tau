package dev.fusemc.tau;

import com.manchickas.optionated.Option;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/// The Tau's entrypoint.
///
/// **Tau** (τ) is a runtime [Polyglot](https://www.graalvm.org/latest/reference-manual/polyglot-programming/) [Value] type-validation library, built
/// originally for [Fuse](https://fusemc.dev).
public final class Tau {

    @ApiStatus.Internal
    private static final @Nullable Object UNDEFINED_SENTINEL = Tau.loadUndefined();

    private Tau() {
        throw new UnsupportedOperationException();
    }

    /// Attempts to parse the provided [Value] using the provided
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
    public static <T> T expect(@NotNull Template<T> template, @NotNull Value value) {
        Objects.requireNonNull(template);
        Objects.requireNonNull(value);
        var option = template.lower(value);
        if (option instanceof Option.Some<T>(var result))
            return result;
        throw new TypeException(template.description(Scope.hashScope()), Tau.describe(value));
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

    public static @NotNull Description describe(@NotNull Value value) {
        return Tau.describe(value, Scope.hashScope(), true);
    }

    @ApiStatus.Internal
    private static @NotNull Description describe(@NotNull Value value,
                                                 @NotNull Scope<Value> visited,
                                                 boolean constant) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(visited);
        if (value.isNumber()) {
            if (constant) {
                if (value.fitsInByte())
                    return Description.BYTE;
                if (value.fitsInShort())
                    return Description.SHORT;
                if (value.fitsInInt())
                    return Description.INTEGER;
                if (value.fitsInLong())
                    return Description.LONG;
                if (value.fitsInBigInteger())
                    return Description.BIG_INTEGER;
                if (value.fitsInFloat())
                    return Description.FLOAT;
                return Description.DOUBLE;
            }
            return Description.NUMBER;
        }
        if (value.isString()) {
            if (constant)
                return Description.literal(value.asString());
            return Description.STRING;
        }
        if (value.isBoolean())
            return Description.BOOLEAN;
        if (Tau.isUndefined(value))
            return Description.UNDEFINED;
        if (Tau.isNull(value))
            return Description.NULL;
        if (value.hasArrayElements()) {
            if (visited.add(value)) {
                var length = (int) value.getArraySize();
                if (length == 0)
                    return Description.concat(
                            Description.ANY,
                            Description.delimiter("[]")
                    );
                if (length <= 5) {
                    var buffer = new Description[length];
                    for (var i = 0; i < length; i++) {
                        var element = value.getArrayElement(i);
                        buffer[i] = Tau.describe(element, visited.branch(), constant);
                    }
                    return Description.concat(
                            Description.delimiter('['),
                            Description.join(Description.delimiter(", "), buffer),
                            Description.delimiter(']')
                    );
                }
                var buffer = new LinkedHashSet<Description>();
                for (var i = 0; i < length; i++) {
                    var element = value.getArrayElement(i);
                    var description = Tau.describe(element, visited.branch(), false);
                    buffer.add(description);
                }
                var result = buffer.toArray(Description[]::new);
                return Description.concat(
                        Description.concat(
                                Description.delimiter('('),
                                Description.join(
                                        Description.delimiter(" | "),
                                        result
                                ),
                                Description.delimiter(')')
                        ),
                        Description.delimiter("[]")
                );
            }
            return Description.UNRESOLVED;
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
                return Description.concat(
                        Description.delimiter('{'),
                        Description.join(Description.delimiter(", "), buffer),
                        Description.delimiter('}')
                );
            }
            return Description.UNRESOLVED;
        }
        if (value.isHostObject()) {
            var wrapped = value.asHostObject();
            if (wrapped instanceof Value[] array) {
                if (array.length == 0)
                    return Description.concat(
                            Description.ANY,
                            Description.delimiter("[]")
                    );
                if (array.length <= 5) {
                    var buffer = new Description[array.length];
                    for (var i = 0; i < array.length; i++) {
                        var element = array[i];
                        buffer[i] = Tau.describe(element, visited.branch(), constant);
                    }
                    return Description.concat(
                            Description.delimiter('['),
                            Description.join(Description.delimiter(", "), buffer),
                            Description.delimiter(']')
                    );
                }
                var buffer = new LinkedHashSet<Description>();
                for (var element : array) {
                    var description = Tau.describe(element, visited.branch(), false);
                    buffer.add(description);
                }
                var result = buffer.toArray(Description[]::new);
                return Description.concat(
                        Description.join(
                                Description.delimiter('('),
                                Description.join(
                                        Description.delimiter(" | "),
                                        result
                                ),
                                Description.delimiter(')')
                        ),
                        Description.delimiter("[]")
                );
            }
            if (wrapped instanceof Collection<?> collection) {
                var length = collection.size();
                if (length == 0)
                    return Description.concat(
                            Description.ANY,
                            Description.delimiter("[]")
                    );
                if (length <= 5) {
                    var buffer = new Description[length];
                    var iterator = collection.iterator();
                    for (var i = 0; iterator.hasNext(); i++) {
                        var element = iterator.next();
                        if (element instanceof Value v) {
                            buffer[i] = Tau.describe(v, visited.branch(), constant);
                            continue;
                        }
                        return Description.UNRESOLVED;
                    }
                    return Description.concat(
                            Description.delimiter('['),
                            Description.join(Description.delimiter(", "), buffer),
                            Description.delimiter(']')
                    );
                }
                var buffer = new LinkedHashSet<Description>();
                for (var element : collection) {
                    if (element instanceof Value v) {
                        var description = Tau.describe(v, visited.branch(), false);
                        buffer.add(description);
                        continue;
                    }
                    return Description.UNRESOLVED;
                }
                var result = buffer.toArray(Description[]::new);
                return Description.concat(
                        Description.concat(
                                Description.delimiter('('),
                                Description.join(
                                        Description.delimiter(" | "),
                                        result
                                ),
                                Description.delimiter(')')
                        ),
                        Description.delimiter("[]")
                );
            }
            if (wrapped instanceof Map<?, ?> map) {
                var buffer = new Description[map.size()];
                var i = 0;
                for (var entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        if (entry.getValue() instanceof Value member) {
                            var description = Tau.describe(member, visited.branch(), constant);
                            buffer[i++] = Description.concat(
                                    Description.literal(key),
                                    Description.delimiter(": "),
                                    description
                            );
                            continue;
                        }
                        buffer[i++] = Description.concat(
                                Description.literal(key),
                                Description.delimiter(": "),
                                Description.UNRESOLVED
                        );
                    }
                }
                return Description.concat(
                        Description.delimiter('{'),
                        Description.join(Description.delimiter(", "), Arrays.copyOf(buffer, i)),
                        Description.delimiter('}')
                );
            }
            return Description.reference(wrapped.getClass());
        }
        return Description.UNKNOWN;
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
