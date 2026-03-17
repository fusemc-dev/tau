package dev.fusemc.tau.element.constructor;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface MonoConstructor<T, A> {

    @NotNull T construct(A a);
}
