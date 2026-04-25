package dev.fusemc.tau.description;

import dev.fusemc.tau.description.type.Attached;
import dev.fusemc.tau.description.type.Join;
import dev.fusemc.tau.description.type.Concat;
import dev.fusemc.tau.description.type.primitive.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public interface Description {

    @NotNull Description NUMBER      = Description.keyword("number");
    @NotNull Description BYTE        = Description.keyword("byte");
    @NotNull Description SHORT       = Description.keyword("short");
    @NotNull Description INTEGER     = Description.keyword("integer");
    @NotNull Description LONG        = Description.keyword("long");
    @NotNull Description FLOAT       = Description.keyword("float");
    @NotNull Description DOUBLE      = Description.keyword("double");
    @NotNull Description BIG_INTEGER = Description.keyword("bigint");
    @NotNull Description STRING      = Description.keyword("string");
    @NotNull Description BOOLEAN     = Description.keyword("boolean");
    @NotNull Description TRUE        = Description.keyword("true");
    @NotNull Description FALSE       = Description.keyword("false");
    @NotNull Description NULL        = Description.keyword("null");
    @NotNull Description UNDEFINED   = Description.keyword("undefined");
    @NotNull Description ANY         = Description.keyword("any");
    @NotNull Description UNKNOWN     = Description.keyword("unknown");
    @NotNull Description ELLIPSIS    = Description.delimiter("...");

    static @NotNull Description attach(@NotNull Description description, @NotNull Domain domain) {
        Objects.requireNonNull(description);
        Objects.requireNonNull(domain);
        return new Attached(description, domain);
    }

    static @NotNull Description delimiter(char delimiter) {
        return new Delimiter(String.valueOf(delimiter));
    }

    static @NotNull Description delimiter(@NotNull String delimiter) {
        Objects.requireNonNull(delimiter);
        return new Delimiter(delimiter);
    }

    static @NotNull Description keyword(@NotNull String keyword) {
        Objects.requireNonNull(keyword);
        return new Keyword(keyword);
    }

    static @NotNull Description literal(@NotNull String literal) {
        Objects.requireNonNull(literal);
        return new Literal(literal);
    }

    static <N extends Number> @NotNull Description number(@NotNull N number) {
        Objects.requireNonNull(number);
        return new Numerical<>(number);
    }

    static @NotNull Description reference(@NotNull Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return new Reference(clazz);
    }

    static @NotNull Description concat(@NotNull Description @NotNull... descriptions) {
        Objects.requireNonNull(descriptions);
        return new Concat(Arrays.copyOf(descriptions, descriptions.length));
    }

    static @NotNull Description join(@NotNull Description delimiter, @NotNull Description @NotNull... descriptions) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(descriptions);
        return new Join(delimiter, Arrays.copyOf(descriptions, descriptions.length));
    }

    @NotNull String stringify(@Nullable Domain enclosing);
}
