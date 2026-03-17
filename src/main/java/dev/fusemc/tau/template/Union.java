package dev.fusemc.tau.template;

import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import com.manchickas.optionated.Option;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Union<T>(@NotNull Template<T> @NotNull[] alternatives) implements Template<T> {

    public Union {
        Objects.requireNonNull(alternatives);
    }

    @Override
    public @NotNull Option<T> parse(@NotNull Value value) {
        for (var alternative : this.alternatives) {
            var option = alternative.parse(value);
            if (option instanceof Option.Some<T>(var result))
                return Option.some(result);
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> serialize(@Nullable T value) {
        for (var alternative : this.alternatives) {
            var option = alternative.serialize(value);
            if (option instanceof Option.Some<Value>(var result))
                return Option.some(result);
        }
        return Option.none();
    }

    @Override
    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        var buffer = new Description[this.alternatives.length];
        for (var i = 0; i < this.alternatives.length; i++) {
            var alternative = this.alternatives[i];
            buffer[i] = alternative.description(points);
        }
        return Description.union(buffer);
    }
}
