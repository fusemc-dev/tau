package dev.fusemc.tau.description.type.primitive;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Origin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Keyword(@NotNull String keyword) implements Description {

    static final Style STYLE = Charcoal.foreground(0xCF8E6D);

    public Keyword {
        Objects.requireNonNull(keyword);
    }

    @Override
    public @NotNull String stringify(@Nullable Origin enclosing) {
        return Keyword.STYLE.wrap(this.keyword);
    }
}
