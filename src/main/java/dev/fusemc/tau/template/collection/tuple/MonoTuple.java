package dev.fusemc.tau.template.collection.tuple;

import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.element.Element;
import dev.fusemc.tau.element.constructor.MonoConstructor;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record MonoTuple<T, A>(@NotNull Element<T, A> a,
                              @NotNull MonoConstructor<T, A> constructor) implements Tuple<T> {

    public MonoTuple {
        Objects.requireNonNull(a);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T> parse(@NotNull Value value) {
        if (Tuple.isTuple(value, 1))
            return this.a.parse(value, 0)
                    .map(this.constructor::construct);
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> serialize(@Nullable T value) {
        if (value != null)
            return Tuple.serializeTuple(value, this.a);
        return Option.none();
    }

    @Override
    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.tuple(this.a.description(points));
    }
}
