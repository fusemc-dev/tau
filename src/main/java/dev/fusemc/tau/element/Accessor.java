package dev.fusemc.tau.element;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Accessor<T, A> {

    A access(@NotNull T instance);
}
