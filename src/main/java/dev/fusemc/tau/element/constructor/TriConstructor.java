package dev.fusemc.tau.element.constructor;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TriConstructor<T, A, B, C> {

    @NotNull T construct(A a, B b, C c);
}
