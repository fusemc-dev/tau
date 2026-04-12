package dev.fusemc.tau;

import dev.fusemc.tau.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TypeException extends RuntimeException {

    private final @NotNull Description present;
    private final @NotNull Description expected;

    public TypeException(@NotNull Description present,
                         @NotNull Description expected) {
        super((String) null);
        this.expected = Objects.requireNonNull(expected);
        this.present = Objects.requireNonNull(present);
    }

    @Override
    public @NotNull String getMessage() {
        return String.format("Type '%s' is not assignable to type '%s'.", this.present.stringify(null), this.expected.stringify(null));
    }

    public @NotNull Description expected() {
        return this.expected;
    }

    public @NotNull Description present() {
        return this.present;
    }
}
