package dev.fusemc.tau.description.dictionary.record;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public sealed interface PropertyDescription extends Description {

    @NotNull Style STYLE = Charcoal.foreground(0xC77DBB);

    record Required(@NotNull String name,
                    @NotNull Description description) implements PropertyDescription {

        public Required {
            Objects.requireNonNull(name);
            Objects.requireNonNull(description);
        }

        @Override
        public @NotNull String stringify(@NotNull Precedence precedence) {
            return PropertyDescription.STYLE.wrap(this.name) + Description.DELIMITER.wrap(": ") + this.description.stringify(Precedence.INFIX);
        }
    }

    record Optional(@NotNull String name,
                    @NotNull Description description) implements PropertyDescription {

        public Optional {
            Objects.requireNonNull(name);
            Objects.requireNonNull(description);
        }

        @Override
        public @NotNull String stringify(@NotNull Precedence precedence) {
            return PropertyDescription.STYLE.wrap(this.name) + Description.DELIMITER.wrap("?: ") + this.description.stringify(Precedence.INFIX);
        }
    }
}
