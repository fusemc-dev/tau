package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Literal(@NotNull String literal) implements Template<String> {

    public Literal {
        Objects.requireNonNull(literal);
    }

    @Override
    public @NotNull Option<String> lower(@NotNull Value value) {
        if (value.isString()) {
            var string = value.asString();
            if (this.literal.equals(string))
                return Option.some(string);
            return Option.none();
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable String value) {
        if (this.literal.equals(value))
            return Template.STRING.raise(value);
        return Option.none();
    }

    @Override
    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.literal(this.literal);
    }
}
