package dev.fusemc.tau;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public final class Scope<T> {

    private final @Nullable Scope<T> parent;
    private final @NotNull Supplier<@NotNull Set<T>> constructor;
    private final @NotNull Set<T> delegate;

    public Scope(@NotNull Supplier<@NotNull Set<T>> constructor) {
        this(constructor, null);
    }

    public static <T> @NotNull Scope<T> hashScope() {
        return new Scope<>(HashSet::new);
    }

    private Scope(@NotNull Supplier<@NotNull Set<T>> constructor,
                  @Nullable Scope<T> parent) {
        this.parent = parent;
        this.constructor = Objects.requireNonNull(constructor);
        this.delegate = this.constructor.get();
    }

    public boolean add(@NotNull T value) {
        Objects.requireNonNull(value);
        if (this.contains(value))
            return false;
        this.delegate.add(value);
        return true;
    }

    public boolean contains(@NotNull T value) {
        Objects.requireNonNull(value);
        if (this.delegate.contains(value))
            return true;
        return this.parent != null && this.parent.contains(value);
    }

    public @NotNull Scope<T> branch() {
        return new Scope<>(this.constructor, this);
    }
}
