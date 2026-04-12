package dev.fusemc.tau.description.type.primitive;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Origin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Delimiter(@NotNull String delimiter) implements Description {

    private static final Style STYLE = Charcoal.foreground(0xBCBEC4);

    public Delimiter {
        Objects.requireNonNull(delimiter);
    }

    @Override
    public @NotNull String stringify(@Nullable Origin enclosing) {
        return Delimiter.STYLE.wrap(this.delimiter);
    }
}
