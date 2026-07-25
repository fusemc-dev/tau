package dev.fusemc.tau.template;

import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import com.manchickas.optionated.Option;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/// A [Template] that attempts multiple alternatives until one succeeds.
///
/// ---
///
/// A `Union` accepts a [Value] as long as it satisfies one of the alternatives provided
/// at construction. The alternatives are attempted **in order**. It is, however, rarely wished to
/// attempt multiple alternatives when **raising** a value.
///
/// As such, a union is often used in conjunction with {@link Template#split(Template, Template) Template.split()}.
///
/// ```
/// Template.split(
///     Template.union(...),
///     Template.record(...)
/// )
/// ```
///
/// @since 0.1.0
public record Union<T>(@NotNull Template<T> @NotNull[] alternatives) implements Template<T> {

    public Union {
        Objects.requireNonNull(alternatives);
    }

    @Override
    public @NotNull Option<T> lower(@NotNull Value value) {
        for (var alternative : this.alternatives) {
            var option = alternative.lower(value);
            if (option instanceof Option.Some<T>(var result))
                return Option.some(result);
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        for (var alternative : this.alternatives) {
            var option = alternative.raise(value);
            if (option instanceof Option.Some<Value>(var result))
                return Option.some(result);
        }
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.join(
                Description.delimiter(" | "),
                Arrays.stream(this.alternatives)
                    .map(t -> t.describe(points))
                    .toArray(Description[]::new)
        );
    }

    @Override
    public @NotNull String toString() {
        return Template.toString(this);
    }
}
