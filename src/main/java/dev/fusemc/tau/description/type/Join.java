package dev.fusemc.tau.description.type;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public record Join(@NotNull Description delimiter,
                   @NotNull Description @NotNull[] descriptions) implements Description {

    public Join {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(descriptions);
    }

    @Override
    public @NotNull String stringify(@Nullable Domain enclosing) {
        var buffer = new StringBuilder();
        for (var i = 0; i < this.descriptions.length; i++) {
            var description = this.descriptions[i];
            if (i > 0)
                buffer.append(this.delimiter.stringify(enclosing));
            buffer.append(description.stringify(enclosing));
        }
        return buffer.toString();
    }

    @Override
    @SuppressWarnings("PatternVariableHidesField")
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Join(var delimiter, var descriptions))
            return this.delimiter.equals(delimiter)
                    && Arrays.equals(this.descriptions, descriptions);
        return false;
    }

    @Override
    public int hashCode() {
        return (this.delimiter.hashCode() * 31) + Arrays.hashCode(this.descriptions);
    }
}
