package dev.fusemc.tau.template.dictionary.record;

import dev.fusemc.tau.Template;
import dev.fusemc.tau.element.property.Property;
import com.manchickas.optionated.Option;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public interface Record<T> extends Template<T> {

    @SafeVarargs
    static <T> @NotNull Option<Value> serializeRecord(@NotNull T instance,
                                                      @NotNull Property<T, ?> @NotNull... properties) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(properties);
        var buffer = new HashMap<>(properties.length);
        for (var property : properties) {
            var name = property.name();
            var option = property.serialize(instance);
            if (option instanceof Option.Some<Value>(var result)) {
                buffer.put(name, result);
                continue;
            }
            return Option.none();
        }
        return Option.some(Value.asValue(Map.copyOf(buffer)));
    }
}
