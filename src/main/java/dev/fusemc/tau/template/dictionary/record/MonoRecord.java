package dev.fusemc.tau.template.dictionary.record;

import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.description.Domain;
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
    public @NotNull Option<T> lower(@NotNull Value value) {
        if (Record.isRecord(value))
            return this.a.lower(value)
                    .map(this.constructor::construct);
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null)
            return Record.raise(value, this.a);
        return Option.none();
    }

    @Override
    public @NotNull Option<Value> raiseWith(@Nullable T instance, @NotNull Property<? super T, ?> property) {
        if (instance != null)
            return Record.raise(instance, property, this.a);
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Record.description(points, this.a), Domain.TEMPLATE);
    }

    @Override
    public @NotNull String toString() {
        return Template.toString(this);
    }
}
