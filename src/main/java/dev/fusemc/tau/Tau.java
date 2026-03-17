package dev.fusemc.tau;

import dev.fusemc.tau.description.dictionary.record.Property;
import com.manchickas.optionated.Option;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class Tau {

    @ApiStatus.Internal
    private static final @Nullable Object UNDEFINED_SENTINEL = Tau.loadUndefined();

    private Tau() {
        throw new UnsupportedOperationException();
    }

    public static <T> T expect(@NotNull Template<T> template, @NotNull Value value) {
        Objects.requireNonNull(template);
        Objects.requireNonNull(value);
        var option = template.parse(value);
        if (option instanceof Option.Some<T>(var result))
            return result;
        throw new TypeException(template.description(), Tau.describe(value));
    }

    public static @NotNull Value undefined() {
        return Value.asValue(Tau.UNDEFINED_SENTINEL);
    }

    public static boolean isUndefined(@NotNull Value value) {
        Objects.requireNonNull(value);
        if (Tau.UNDEFINED_SENTINEL != null)
            return value.isNull() && value.hashCode() == Tau.UNDEFINED_SENTINEL.hashCode();
        return false;
    }

    public static boolean isNull(@NotNull Value value) {
        Objects.requireNonNull(value);
        if (Tau.UNDEFINED_SENTINEL != null)
            return value.isNull() && value.hashCode() != Tau.UNDEFINED_SENTINEL.hashCode();
        return value.isNull();
    }

    public static @NotNull Description describe(@NotNull Value value) {
        return Tau.describe(value, Scope.hashScope());
    }

    @ApiStatus.Internal
    private static @NotNull Description describe(@NotNull Value value,
                                                 @NotNull Scope<Value> visited) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(visited);
        if (value.isNumber())
            return Description.NUMBER;
        if (value.isString())
            return Description.literal(value.asString());
        if (value.isBoolean())
            return Description.BOOLEAN;
        if (Tau.isUndefined(value))
            return Description.UNDEFINED;
        if (Tau.isNull(value))
            return Description.NULL;
        if (value.hasArrayElements()) {
            if (visited.add(value)) {
                var length = (int) value.getArraySize();
                if (length <= 5) {
                    var buffer = new Description[length];
                    for (var i = 0; i < length; i++) {
                        var element = value.getArrayElement(i);
                        buffer[i] = Tau.describe(element, visited.branch());
                    }
                    return Description.tuple(buffer);
                }
                var buffer = new LinkedHashSet<Description>();
                for (var i = 0; i < length; i++) {
                    var element = value.getArrayElement(i);
                    var description = Tau.describe(element, visited.branch());
                    buffer.add(description);
                }
                var result = buffer.toArray(Description[]::new);
                return Description.array(Description.union(result));
            }
            return Description.UNRESOLVED;
        }
        if (value.hasMembers()) {
            if (visited.add(value)) {
                var keys = value.getMemberKeys();
                var buffer = new Property[keys.size()];
                var i = 0;
                for (var key : keys) {
                    var member = value.getMember(key);
                    var description = Tau.describe(member, visited.branch());
                    buffer[i++] = Description.property(key, description);
                }
                return Description.record(buffer);
            }
            return Description.UNRESOLVED;
        }
        if (value.isHostObject()) {
            var wrapped = value.asHostObject();
            if (wrapped instanceof Value[] array) {
                if (array.length <= 5) {
                    var buffer = new Description[array.length];
                    for (var i = 0; i < array.length; i++) {
                        var element = array[i];
                        buffer[i] = Tau.describe(element, visited.branch());
                    }
                    return Description.tuple(buffer);
                }
                var buffer = new LinkedHashSet<Description>();
                for (var element : array) {
                    var description = Tau.describe(element, visited.branch());
                    buffer.add(description);
                }
                var result = buffer.toArray(Description[]::new);
                return Description.array(Description.union(result));
            }
            if (wrapped instanceof Collection<?> collection) {
                var length = collection.size();
                if (length <= 5) {
                    var buffer = new Description[length];
                    var iterator = collection.iterator();
                    for (var i = 0; iterator.hasNext(); i++) {
                        var element = iterator.next();
                        if (element instanceof Value v) {
                            buffer[i] = Tau.describe(v, visited.branch());
                            continue;
                        }
                        return Description.UNRESOLVED;
                    }
                    return Description.tuple(buffer);
                }
                var buffer = new LinkedHashSet<Description>();
                var iterator = collection.iterator();
                for (var i = 0; iterator.hasNext(); i++) {
                    var element = iterator.next();
                    if (element instanceof Value v) {
                        var description = Tau.describe(v, visited.branch());
                        buffer.add(description);
                        continue;
                    }
                    return Description.UNRESOLVED;
                }
                var result = buffer.toArray(Description[]::new);
                return Description.array(Description.union(result));
            }
            if (wrapped instanceof Map<?, ?> map) {
                var buffer = new Property[map.size()];
                var i = 0;
                for (var entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        if (entry.getValue() instanceof Value member) {
                            var description = Tau.describe(member, visited.branch());
                            buffer[i++] = Description.property(key, description);
                            continue;
                        }
                        return Description.UNRESOLVED;
                    }
                    return Description.UNRESOLVED;
                }
                return Description.record(buffer);
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
