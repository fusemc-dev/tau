package dev.fusemc.tau.template.collection.tuple;

import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.element.Element;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public interface Tuple<T> extends Template<T> {

    static boolean isTuple(@NotNull Value value, int length) {
        Objects.requireNonNull(value);
        if (value.hasArrayElements())
            return value.getArraySize() == length;
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (host instanceof Value[] values)
                return values.length == length;
            if (host instanceof Collection<?> collection)
                return collection.size() == length;
            return false;
        }
        return false;
    }

    @SafeVarargs
    static <T> @NotNull Option<Value> serialize(@NotNull T instance,
                                                @NotNull Element<T, ?> @NotNull... elements) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(elements);
        var buffer = new Value[elements.length];
        for (var i = 0; i < elements.length; i++) {
            var element = Objects.requireNonNull(elements[i]);
            var option = element.raise(instance);
            if (option instanceof Option.Some<Value>(var result)) {
                buffer[i] = result;
                continue;
            }
            return Option.none();
        }
        return Option.some(Value.asValue(buffer));
    }

    static @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points,
                                            @NotNull Element<?, ?> @NotNull... elements) {
        Objects.requireNonNull(elements);
        Objects.requireNonNull(points);
        return Description.concat(
                Description.delimiter('['),
                Description.join(Description.delimiter(", "), Arrays.stream(elements)
                        .map(el -> el.description(points))
                        .toArray(Description[]::new)),
                Description.delimiter(']')
        );
    }
}
