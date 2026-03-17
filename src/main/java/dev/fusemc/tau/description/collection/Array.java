package dev.fusemc.tau.description.collection;

import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Array(@NotNull Description description) implements Description {

    public Array {
        Objects.requireNonNull(description);
    }

    @Override
    public @NotNull String stringify(@NotNull Precedence precedence) {
        return this.description.stringify(Precedence.POSTFIX) + Description.DELIMITER.wrap("[]");
    }
}
