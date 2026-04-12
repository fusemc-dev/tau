package dev.fusemc.tau.description.type;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Origin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Attached(@NotNull Description description,
                       @NotNull Origin origin) implements Description {

    public Attached {
        Objects.requireNonNull(description);
        Objects.requireNonNull(origin);
    }

    @Override
    public @NotNull String stringify(@Nullable Origin enclosing) {
        if (this.origin.equals(enclosing))
            return this.description.stringify(this.origin);
        return this.origin.annotate(this.description);
    }
}
