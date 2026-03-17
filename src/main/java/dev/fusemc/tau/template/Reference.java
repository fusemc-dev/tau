package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Reference<T>(@NotNull Class<T> type) implements Template<@NotNull T> {

    public Reference {
        Objects.requireNonNull(type);
    }

    @Override
    public @NotNull Option<@NotNull T> parse(@NotNull Value value) {
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (this.type.isInstance(host))
                return Option.some(this.type.cast(host));
            return Option.none();
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> serialize(@Nullable T value) {
        if (value != null)
            return Option.some(Value.asValue(value));
        return Option.none();
    }

    @Override
    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.reference(this.type);
    }
}
