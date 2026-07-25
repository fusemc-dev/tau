package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// A [Template] that accepts a reference to an instance of some type `T`.
///
/// ---
/// A `Reference` accepts a [Value] that wraps either a **proxy** or a **host value** of type `T` and directly
/// converts it. It is most commonly used in conjunction with other [Template]s:
///
/// ```
/// Template.union(
///     Template.record(...),
///     Template.reference(Person.class)
/// )
/// ```
///
/// @since 0.1.0
public record Reference<T>(@NotNull Class<T> type) implements Template<@NotNull T> {

    public Reference {
        Objects.requireNonNull(type);
    }

    @Override
    public @NotNull Option<@NotNull T> lower(@NotNull Value value) {
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (this.type.isInstance(host))
                return Option.some(this.type.cast(host));
            return Option.none();
        }
        if (value.isProxyObject()) {
            var proxy = value.asProxyObject();
            if (this.type.isInstance(proxy))
                return Option.some(this.type.cast(proxy));
            return Option.none();
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null)
            return Option.some(Value.asValue(value));
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.reference(this.type);
    }

    @Override
    public @NotNull String toString() {
        return Template.toString(this);
    }
}
