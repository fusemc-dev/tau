package dev.fusemc.tau;

import dev.fusemc.tau.description.Description;
import org.jetbrains.annotations.NotNull;

public interface Inspectable {

    @NotNull Description inspect();
}
