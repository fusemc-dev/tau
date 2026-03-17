package dev.fusemc.tau;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.description.Keyword;
import dev.fusemc.tau.description.Literal;
import dev.fusemc.tau.description.Reference;
import dev.fusemc.tau.description.Union;
import dev.fusemc.tau.description.collection.Array;
import dev.fusemc.tau.description.collection.Tuple;
import dev.fusemc.tau.description.dictionary.record.PropertyDescription;
import dev.fusemc.tau.description.dictionary.record.Record;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@FunctionalInterface
public interface Description {

    @NotNull Style DELIMITER        = Charcoal.foreground(0xBCBEC4);

    @NotNull Description NUMBER     = Description.keyword("number");
    @NotNull Description STRING     = Description.keyword("string");
    @NotNull Description BOOLEAN    = Description.keyword("boolean");
    @NotNull Description NULL       = Description.keyword("null");
    @NotNull Description UNDEFINED  = Description.keyword("undefined");
    @NotNull Description ANY        = Description.keyword("any");
    @NotNull Description UNKNOWN    = Description.keyword("unknown");
    @NotNull Description UNRESOLVED = (_) -> Description.DELIMITER.wrap("...");

    static @NotNull Description keyword(@NotNull String keyword) {
        Objects.requireNonNull(keyword);
        return new Keyword(keyword);
    }

    static @NotNull Description literal(@NotNull String literal) {
        Objects.requireNonNull(literal);
        return new Literal(literal);
    }

    static @NotNull Description reference(@NotNull Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return new Reference(clazz);
    }

    static @NotNull Description union(@NotNull Description @NotNull... alternatives) {
        Objects.requireNonNull(alternatives);
        return new Union(Arrays.copyOf(alternatives, alternatives.length));
    }

    static @NotNull Description array(@NotNull Description element) {
        Objects.requireNonNull(element);
        return new Array(element);
    }

    static @NotNull PropertyDescription.Optional optional(@NotNull PropertyDescription.Required property) {
        Objects.requireNonNull(property);
        return new PropertyDescription.Optional(property.name(), property.description());
    }

    static @NotNull PropertyDescription.Required property(@NotNull String name, @NotNull Description description) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        return new PropertyDescription.Required(name, description);
    }

    static @NotNull Description record(@NotNull PropertyDescription @NotNull... properties) {
        Objects.requireNonNull(properties);
        return new Record(properties);
    }

    static @NotNull Description tuple(@NotNull Description @NotNull... elements) {
        Objects.requireNonNull(elements);
        return new Tuple(elements);
    }

    default @NotNull String stringify() {
        return this.stringify(Precedence.INFIX);
    }

    @NotNull String stringify(@NotNull Precedence precedence);

    enum Precedence {

        INFIX,
        POSTFIX;

        public boolean isTighterThan(@NotNull Precedence other) {
            Objects.requireNonNull(other);
            return this.ordinal() > other.ordinal();
        }
    }
}
