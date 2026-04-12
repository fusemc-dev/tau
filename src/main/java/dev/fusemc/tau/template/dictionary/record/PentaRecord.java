package dev.fusemc.tau.template.dictionary.record;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.description.Origin;
import dev.fusemc.tau.element.Property;
import dev.fusemc.tau.element.constructor.PentaConstructor;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record PentaRecord<T, A, B, C, D, E>(
        @NotNull Property<T, A> a,
        @NotNull Property<T, B> b,
        @NotNull Property<T, C> c,
        @NotNull Property<T, D> d,
        @NotNull Property<T, E> e,
        @NotNull PentaConstructor<T, A, B, C, D, E> constructor
) implements Record<T> {

    public PentaRecord {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(c);
        Objects.requireNonNull(d);
        Objects.requireNonNull(e);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T> lower(@NotNull Value value) {
        if (Record.isRecord(value))
            return this.a.lower(value)
                    .flatMap(a -> this.b.lower(value)
                            .flatMap(b -> this.c.lower(value)
                                    .flatMap(c -> this.d.lower(value)
                                            .flatMap(d -> this.e.lower(value)
                                                    .map(e -> this.constructor.construct(a, b, c, d, e))))));
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null)
            return Record.raise(value, this.a, this.b, this.c, this.d, this.e);
        return Option.none();
    }

    @Override
    public @NotNull Option<Value> raiseWith(@Nullable T instance, @NotNull Property<? super T, ?> property) {
        if (instance != null)
            return Record.raise(instance, property, this.a, this.b, this.c, this.d, this.e);
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Record.description(points, this.a, this.b, this.c, this.d, this.e), Origin.SCHEMA);
    }
}
