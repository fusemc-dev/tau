package dev.fusemc.tau;

import org.jetbrains.annotations.NotNull;

/// A type inspectable through `Tau.inspect()`.
///
/// ---
/// When inspecting an `Inspectable` type, its [#inspect()] implementation
/// will used over producing a plain `[]`
public interface Inspectable {

    @NotNull Description inspect();
}
