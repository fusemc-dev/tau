package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Domain;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

public final class Mu<T> implements Template<T> {

    private final @NotNull Template<T> delegate;

    public Mu(@NotNull Function<Template<T>, Template<T>> constructor) {
        Objects.requireNonNull(constructor);
        this.delegate = Objects.requireNonNull(constructor.apply(this));
    }

    @Override
    @SuppressWarnings("ConstantValue")
    public @NotNull Option<T> lower(@NotNull Value value) {
        if (this.delegate == null)
            throw new AssertionError("Attempted to parse() a Mu within the constructor function.");
        return this.delegate.lower(value);
    }

    @Override
    @SuppressWarnings("ConstantValue")
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (this.delegate == null)
            throw new AssertionError("Attempted to serialize() a Mu within the constructor function.");
        return this.delegate.raise(value);
    }

    @Override
    @SuppressWarnings("ConstantValue")
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        if (this.delegate == null)
            throw new AssertionError("Attempted to describe() a Mu within the constructor function.");
        if (points.add(this))
            return Description.attach(this.delegate.describe(points.branch()), Domain.TEMPLATE);
        return Description.attach(Description.ELLIPSIS, Domain.TEMPLATE);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Mu<?> other)
            return this.delegate.equals(other.delegate);
        return false;
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return String.format("Mu[%s]", this.delegate);
    }
}
