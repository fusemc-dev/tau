package dev.fusemc.tau.description.type;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Origin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public record Concat(@NotNull Description @NotNull[] descriptions) implements Description {

    public Concat {
        Objects.requireNonNull(descriptions);
    }

    @Override
    public @NotNull String stringify(@Nullable Origin enclosing) {
        var buffer = new StringBuilder();
        for (var description : this.descriptions)
            buffer.append(description.stringify(enclosing));
        return buffer.toString();
    }

    @Override
    @SuppressWarnings("PatternVariableHidesField")
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Concat(var descriptions))
            return Arrays.equals(this.descriptions, descriptions);
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.descriptions);
    }
}
