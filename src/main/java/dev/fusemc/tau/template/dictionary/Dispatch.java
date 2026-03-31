package dev.fusemc.tau.template.dictionary;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.element.Property;
import dev.fusemc.tau.template.Mu;
import dev.fusemc.tau.template.dictionary.record.Record;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

public record Dispatch<T, A>(
        @NotNull Property<T, A> discriminant,
        @NotNull Function<A, Option<Record<? extends T>>> dispatch
) implements Template<T> {

    public Dispatch {
        Objects.requireNonNull(discriminant);
        Objects.requireNonNull(dispatch);
    }

    @Override
    @SuppressWarnings("PatternVariableHidesField")
    public @NotNull Option<T> lower(@NotNull Value value) {
        if (Record.isRecord(value)) {
            var option = this.discriminant.lower(value);
            if (option instanceof Option.Some<A>(var discriminant)) {
                var delegate = this.dispatch.apply(discriminant);
                return delegate.flatMap(d -> d.lower(value)
                        .map(t -> t));
            }
            return Option.none();
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null) {
            var delegate = this.dispatch.apply(this.discriminant.access(value));
            return delegate.flatMap(d -> this.raiseUnsafe(value, d));
        }
        return Option.none();
    }

    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    private <V extends T> @NotNull Option<@NotNull Value> raiseUnsafe(@NotNull T value,
                                                                      @NotNull Record<V> delegate) {
        try {
            return delegate.raiseWith((V) value, this.discriminant);
        } catch (ClassCastException e) {
            return Option.none();
        }
    }

    @Override
    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.concat(
                Description.delimiter('{'),
                Description.concat(
                        this.discriminant.description(points),
                        Description.delimiter(", "),
                        Description.UNRESOLVED
                ),
                Description.delimiter('}')
        );
    }
}
