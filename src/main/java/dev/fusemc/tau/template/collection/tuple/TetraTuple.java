package dev.fusemc.tau.template.collection.tuple;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.element.Element;
import dev.fusemc.tau.element.constructor.TetraConstructor;
import dev.fusemc.tau.element.constructor.TriConstructor;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record TetraTuple<T, A, B, C, D>(@NotNull Element<T, A> a,
                                     @NotNull Element<T, B> b,
                                     @NotNull Element<T, C> c,
                                     @NotNull Element<T, D> d,
                                     @NotNull TetraConstructor<T, A, B, C, D> constructor) implements Tuple<T> {

    public TetraTuple {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(c);
        Objects.requireNonNull(d);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T> parse(@NotNull Value value) {
        if (Tuple.isTuple(value, 4))
            return this.a.parse(value, 0)
                    .flatMap(a -> this.b.parse(value, 1)
                            .flatMap(b -> this.c.parse(value, 2)
                                    .flatMap(c -> this.d.parse(value, 3)
                                            .map(d -> this.constructor.construct(a, b, c, d)))));
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> serialize(@Nullable T value) {
        if (value != null)
            return Tuple.serializeTuple(value, this.a, this.b, this.c, this.d);
        return Option.none();
    }

    @Override
    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.tuple(
                this.a.description(points),
                this.b.description(points),
                this.c.description(points),
                this.d.description(points)
        );
    }
}
