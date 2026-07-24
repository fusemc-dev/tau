package dev.fusemc.tau.description.primitive;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Delimiter(@NotNull String delimiter) implements Description {

    private static final Style STYLE = Charcoal.foreground(0xBCBEC4);

    public Delimiter {
        Objects.requireNonNull(delimiter);
    }

    @Override
    public @NotNull String stringify() {
        return Delimiter.STYLE.wrap(this.delimiter);
    }
}
