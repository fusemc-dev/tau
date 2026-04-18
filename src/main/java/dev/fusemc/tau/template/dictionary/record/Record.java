package dev.fusemc.tau.template.dictionary.record;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.element.Property;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.proxy.Dictionary;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public interface Record<T> extends Template<T> {

    @NotNull Option<Value> raiseWith(
            @Nullable T instance,
            @NotNull Property<? super T, ?> property
    );

    static boolean isRecord(@NotNull Value value) {
        Objects.requireNonNull(value);
        if (value.hasMembers() && !value.hasArrayElements())
            return true;
        if (value.isHostObject()) {
            var host = value.asHostObject();
            return host instanceof Map<?,?>;
        }
        return false;
    }

    @SafeVarargs
    static <T> @NotNull Option<Value> raise(@NotNull T instance,
                                            @NotNull Property<? super T, ?> @NotNull... properties) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(properties);
        var buffer = Dictionary.builder(properties.length);
        for (var property : properties) {
            var name = property.name();
            var option = property.raise(instance);
            if (option instanceof Option.Some<Value>(var result)) {
                buffer.append(name, result);
                continue;
            }
            return Option.none();
        }
        var dict = buffer.build();
        return Option.some(Value.asValue(dict));
    }

    static @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points,
                                            @NotNull Property<?, ?> @NotNull... properties) {
        return Description.concat(
                Description.delimiter('{'),
                Description.join(Description.delimiter(", "), Arrays.stream(properties)
                        .map(property -> property.description(points))
                        .toArray(Description[]::new)),
                Description.delimiter('}')
        );
    }
}
