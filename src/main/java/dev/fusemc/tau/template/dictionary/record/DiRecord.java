package dev.fusemc.tau.template.dictionary.record;

import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.element.constructor.DiConstructor;
import dev.fusemc.tau.element.property.Property;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record DiRecord<T, A, B>(
        @NotNull Property<T, A> a,
        @NotNull Property<T, B> b,
        @NotNull DiConstructor<T, A, B> constructor
) implements Record<T> {

    public DiRecord {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T> parse(@NotNull Value value) {
        return this.a.parse(value)
                .flatMap(a -> this.b.parse(value)
                        .map(b -> this.constructor.construct(a, b)));
    }

    @Override
    public @NotNull Option<@NotNull Value> serialize(@Nullable T value) {
        if (value != null)
            return Record.serializeRecord(value, this.a, this.b);
        return Option.none();
    }

    @Override
    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.record(this.a.description(points), this.b.description(points));
    }
}
