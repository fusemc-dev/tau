package dev.fusemc.tau.description.type;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Attached(@NotNull Description description,
                       @NotNull Domain domain) implements Description {

    public Attached {
        Objects.requireNonNull(description);
        Objects.requireNonNull(domain);
    }

    @Override
    public @NotNull String stringify(@Nullable Domain enclosing) {
        if (this.domain.equals(enclosing))
            return this.description.stringify(this.domain);
        return this.domain.annotate(this.description);
    }
}
