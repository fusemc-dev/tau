package dev.fusemc.tau.template.dictionary.record;

import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.element.constructor.MonoConstructor;
import dev.fusemc.tau.element.Property;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record MonoRecord<T, A>(
        @NotNull Property<T, A> a,
        @NotNull MonoConstructor<T, A> constructor
) implements Record<T> {

    public MonoRecord {
        Objects.requireNonNull(a);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T> parse(@NotNull Value value) {
        return this.a.parse(value)
                .map(this.constructor::construct);
    }

    @Override
    public @NotNull Option<@NotNull Value> serialize(@Nullable T value) {
        if (value != null)
            return Record.serializeRecord(value, this.a);
        return Option.none();
    }

    @Override
    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.record(this.a.description(points));
    }
}
