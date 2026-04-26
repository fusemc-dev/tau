package dev.fusemc.tau.template.collection.tuple;

import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.description.Domain;
import dev.fusemc.tau.element.Element;
import dev.fusemc.tau.element.constructor.DiConstructor;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record DiTuple<T, A, B>(@NotNull Element<T, A> a,
                               @NotNull Element<T, B> b,
                               @NotNull DiConstructor<T, A, B> constructor) implements Tuple<T> {

    public DiTuple {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T> lower(@NotNull Value value) {
        if (Tuple.isTuple(value, 2))
            return this.a.lower(value, 0)
                    .flatMap(a -> this.b.lower(value, 1)
                            .map(b -> this.constructor.construct(a, b)));
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null)
            return Tuple.serialize(value, this.a, this.b);
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Tuple.description(points, this.a, this.b), Domain.TEMPLATE);
    }

    @Override
    public @NotNull String toString() {
        return Template.toString(this);
    }
}
