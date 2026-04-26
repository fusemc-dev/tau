package dev.fusemc.tau.template.dictionary.record;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.description.Domain;
import dev.fusemc.tau.element.Property;
import dev.fusemc.tau.element.constructor.TriConstructor;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record TriRecord<T, A, B, C>(
        @NotNull Property<T, A> a,
        @NotNull Property<T, B> b,
        @NotNull Property<T, C> c,
        @NotNull TriConstructor<T, A, B, C> constructor
) implements Record<T> {

    public TriRecord {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(c);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T> lower(@NotNull Value value) {
        if (Record.isRecord(value))
            return this.a.lower(value)
                    .flatMap(a -> this.b.lower(value)
                            .flatMap(b -> this.c.lower(value)
                                    .map(c -> this.constructor.construct(a, b, c))));
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null)
            return Record.raise(value, this.a, this.b, this.c);
        return Option.none();
    }

    @Override
    public @NotNull Option<Value> raiseWith(@Nullable T instance, @NotNull Property<? super T, ?> property) {
        if (instance != null)
            return Record.raise(instance, property, this.a, this.b, this.c);
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Record.description(points, this.a, this.b, this.c), Domain.TEMPLATE);
    }

    @Override
    public @NotNull String toString() {
        return Template.toString(this);
    }
}
