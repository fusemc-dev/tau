package dev.fusemc.tau.element.constructor;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PentaConstructor<T, A, B, C, D, E> {

    @NotNull T construct(A a, B b, C c, D d, E e);
}
