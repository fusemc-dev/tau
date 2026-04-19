package dev.fusemc.tau.description;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface Origin {

    @NotNull Origin HOST = new Origin() {

        private static final Style STYLE = Charcoal.foreground(0x56A8F5);

        @Override
        public @NotNull String annotate(@NotNull Description description) {
            Objects.requireNonNull(description);
            return STYLE.wrap('{') + description.stringify(this) + STYLE.wrap('}');
        }
    };
    @NotNull Origin POLYGLOT = new Origin() {

        private static final Style STYLE = Charcoal.foreground(0x6AAB73);

        @Override
        public @NotNull String annotate(@NotNull Description description) {
            Objects.requireNonNull(description);
            return STYLE.wrap('/') + description.stringify(this) + STYLE.wrap('/');
        }
    };
    @NotNull Origin SCHEMA = new Origin() {

        private static final Style STYLE = Charcoal.foreground(0xC77DBB);

        @Override
        public @NotNull String annotate(@NotNull Description description) {
            Objects.requireNonNull(description);
            return STYLE.wrap('<') + description.stringify(this) + STYLE.wrap('>');
        }
    };
    @NotNull Origin REFLECTION = new Origin() {

        private static final Style STYLE = Charcoal.foreground(0xCF8E6D);

        @Override
        public @NotNull String annotate(@NotNull Description description) {
            Objects.requireNonNull(description);
            return STYLE.wrap('|') + description.stringify(this) + STYLE.wrap('|');
        }
    };

    @NotNull String annotate(@NotNull Description description);
}
