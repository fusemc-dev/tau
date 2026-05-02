package dev.fusemc.tau.description.type.primitive;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Numerical<N extends Number>(@NotNull N number) implements Description {

    private static final @NotNull Style STYLE = Charcoal.foreground(0x2AACB8);

    public Numerical {
        Objects.requireNonNull(number);
    }

    @Override
    public @NotNull String stringify(@Nullable Domain enclosing) {
        if (this.number instanceof Float f) {
            if (f.isInfinite() || f.isNaN())
                return Keyword.STYLE.wrap(f.toString());
            return Numerical.STYLE.wrap(f.toString());
        }
        if (this.number instanceof Double d) {
            if (d.isInfinite() || d.isNaN())
                return Keyword.STYLE.wrap(d.toString());
            return Numerical.STYLE.wrap(d.toString());
        }
        return Numerical.STYLE.wrap(this.number.toString());
    }
}
