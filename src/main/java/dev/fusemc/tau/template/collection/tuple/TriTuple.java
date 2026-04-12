package dev.fusemc.tau.template.collection.tuple;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.description.Origin;
import dev.fusemc.tau.element.Element;
import dev.fusemc.tau.element.constructor.TriConstructor;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record TriTuple<T, A, B, C>(@NotNull Element<T, A> a,
                             @NotNull Element<T, B> b,
                             @NotNull Element<T, C> c,
                             @NotNull TriConstructor<T, A, B, C> constructor) implements Tuple<T> {

    public TriTuple {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(c);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T> lower(@NotNull Value value) {
        if (Tuple.isTuple(value, 3))
            return this.a.lower(value, 0)
                    .flatMap(a -> this.b.lower(value, 1)
                            .flatMap(b -> this.c.lower(value, 2)
                                    .map(c -> this.constructor.construct(a, b, c))));
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null)
            return Tuple.serialize(value, this.a, this.b, this.c);
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Tuple.description(points, this.a, this.b, this.c), Origin.SCHEMA);
    }
}
