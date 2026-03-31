package dev.fusemc.tau;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TypeException extends RuntimeException {

    private final @NotNull Description expected;
    private final @NotNull Description provided;

    public TypeException(@NotNull Description expected,
                         @NotNull Description provided) {
        super((String) null);
        this.expected = Objects.requireNonNull(expected);
        this.provided = Objects.requireNonNull(provided);
    }

    @Override
    public @NotNull String getMessage() {
        return String.format("Type '%s' is not assignable to type '%s'.", this.provided.stringify(), this.expected.stringify());
    }
}
