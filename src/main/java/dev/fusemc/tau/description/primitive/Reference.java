package dev.fusemc.tau.description.primitive;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Reference(@NotNull String type) implements Description {

    private static final @NotNull Style STYLE = Charcoal.foreground(0x56A8F5);

    public Reference {
        Objects.requireNonNull(type);
    }

    @Override
    public @NotNull String stringify() {
        return Reference.STYLE.wrap(this.type);
    }
}
