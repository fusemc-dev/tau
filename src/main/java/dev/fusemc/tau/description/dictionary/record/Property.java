package dev.fusemc.tau.description.dictionary.record;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

public sealed interface Property extends Description {

    @NotNull Style STYLE = Charcoal.foreground(0xC77DBB);

    record Required(@NotNull String name,
                    @NotNull Description description) implements Property {

        @Override
        public @NotNull String stringify(@NotNull Precedence precedence) {
            return Property.STYLE.apply(this.name) + Description.DELIMITER.apply(": ") + this.description.stringify(Precedence.INFIX);
        }
    }

    record Optional(@NotNull String name,
                    @NotNull Description description) implements Property {

        @Override
        public @NotNull String stringify(@NotNull Precedence precedence) {
            return Property.STYLE.apply(this.name) + Description.DELIMITER.apply("?: ") + this.description.stringify(Precedence.INFIX);
        }
    }
}
