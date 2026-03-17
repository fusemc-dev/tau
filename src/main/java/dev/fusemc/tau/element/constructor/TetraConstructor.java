package dev.fusemc.tau.element.constructor;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TetraConstructor<T, A, B, C, D> {

    @NotNull T construct(A a, B b, C c, D d);
}
