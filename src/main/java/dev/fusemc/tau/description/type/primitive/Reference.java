package dev.fusemc.tau.description.type.primitive;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Documented;
import dev.fusemc.tau.description.Domain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Reference(@NotNull String type) implements Description {

    private static final @NotNull Style STYLE = Charcoal.foreground(0x56A8F5);

    public Reference {
        Objects.requireNonNull(type);
    }

    @Override
    public @NotNull String stringify(@Nullable Domain enclosing) {
        return Reference.STYLE.wrap(this.type);
    }
}
