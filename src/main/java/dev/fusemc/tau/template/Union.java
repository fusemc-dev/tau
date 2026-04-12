package dev.fusemc.tau.template;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.description.Origin;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public record Union<T>(@NotNull Template<T> @NotNull[] alternatives) implements Template<T> {

    public Union {
        Objects.requireNonNull(alternatives);
    }

    @Override
    public @NotNull Option<T> lower(@NotNull Value value) {
        for (var alternative : this.alternatives) {
            var option = alternative.lower(value);
            if (option instanceof Option.Some<T>(var result))
                return Option.some(result);
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        for (var alternative : this.alternatives) {
            var option = alternative.raise(value);
            if (option instanceof Option.Some<Value>(var result))
                return Option.some(result);
        }
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Description.join(
                Description.delimiter(" | "),
                Arrays.stream(this.alternatives)
                    .map(t -> t.describe(points))
                    .toArray(Description[]::new)
        ), Origin.SCHEMA);
    }
}
