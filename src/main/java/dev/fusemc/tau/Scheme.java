package dev.fusemc.tau;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Scheme {

    private final @NotNull Style delimiter;
    private final @NotNull Style reference;
    private final @NotNull Style property;
    private final @NotNull Style keyword;
    private final @NotNull Style literal;

    private Scheme(@NotNull Style delimiter,
                   @NotNull Style reference,
                   @NotNull Style property,
                   @NotNull Style keyword,
                   @NotNull Style literal) {
        this.delimiter = Objects.requireNonNull(delimiter);
        this.reference = Objects.requireNonNull(reference);
        this.property  = Objects.requireNonNull(property);
        this.keyword   = Objects.requireNonNull(keyword);
        this.literal   = Objects.requireNonNull(literal);
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public @NotNull Style delimiter() {
        return this.delimiter;
    }

    public @NotNull Style reference() {
        return this.reference;
    }

    public @NotNull Style property() {
        return this.property;
    }

    public @NotNull Style keyword() {
        return this.keyword;
    }

    public @NotNull Style literal() {
        return this.literal;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Scheme other)
            return this.delimiter.equals(other.delimiter) &&
                   this.reference.equals(other.reference) &&
                   this.property.equals(other.property) &&
                   this.keyword.equals(other.keyword) &&
                   this.literal.equals(other.literal);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.delimiter, this.reference, this.property, this.keyword, this.literal);
    }

    public static final class Builder {

        private @NotNull Style delimiter;
        private @NotNull Style reference;
        private @NotNull Style property;
        private @NotNull Style keyword;
        private @NotNull Style literal;

        private Builder() {
            this.delimiter = Charcoal.empty();
            this.reference = Charcoal.empty();
            this.property  = Charcoal.empty();
            this.keyword   = Charcoal.empty();
            this.literal   = Charcoal.empty();
        }

        public @NotNull Builder delimiter(@NotNull Style delimiter) {
            this.delimiter = Objects.requireNonNull(delimiter);
            return this;
        }

        public @NotNull Builder reference(@NotNull Style reference) {
            this.reference = Objects.requireNonNull(reference);
            return this;
        }

        public @NotNull Builder property(@NotNull Style property) {
            this.property = Objects.requireNonNull(property);
            return this;
        }

        public @NotNull Builder keyword(@NotNull Style keyword) {
            this.keyword = Objects.requireNonNull(keyword);
            return this;
        }

        public @NotNull Builder literal(@NotNull Style literal) {
            this.literal = Objects.requireNonNull(literal);
            return this;
        }

        public @NotNull Scheme build() {
            return new Scheme(this.delimiter, this.reference, this.property, this.keyword, this.literal);
        }
    }
}
