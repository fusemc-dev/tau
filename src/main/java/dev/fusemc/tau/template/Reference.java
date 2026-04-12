package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Origin;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Reference<T>(@NotNull Class<T> type) implements Template<@NotNull T> {

    public Reference {
        Objects.requireNonNull(type);
    }

    @Override
    public @NotNull Option<@NotNull T> lower(@NotNull Value value) {
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (this.type.isInstance(host))
                return Option.some(this.type.cast(host));
            return Option.none();
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null)
            return Option.some(Value.asValue(value));
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Description.reference(this.type), Origin.SCHEMA);
    }
}
