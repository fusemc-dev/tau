package dev.fusemc.tau.element.constructor;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface DiConstructor<T, A, B> {

    @NotNull T construct(A a, B b);
}
