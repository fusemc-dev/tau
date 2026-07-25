package dev.fusemc.tau;

import org.jetbrains.annotations.NotNull;

/// A type inspectable through `Tau.inspect()`.
///
/// ---
/// When inspecting an `Inspectable` type, its [#inspect()] implementation
/// will be used over producing a plain `[]`
///
/// @since ~0.2.8
public interface Inspectable {

    @NotNull Description inspect();
}
