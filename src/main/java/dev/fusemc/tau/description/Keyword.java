package dev.fusemc.tau.description;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Keyword(@NotNull String keyword) implements Description {

    public static final Style STYLE = Charcoal.foreground(0xCF8E6D);

    public Keyword {
        Objects.requireNonNull(keyword);
    }

    @Override
    public @NotNull String stringify(@NotNull Precedence precedence) {
        return Keyword.STYLE.wrap(this.keyword);
    }
}
