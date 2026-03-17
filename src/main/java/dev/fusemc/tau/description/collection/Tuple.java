package dev.fusemc.tau.description.collection;

import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Tuple(@NotNull Description @NotNull[] elements) implements Description {

    public Tuple {
        Objects.requireNonNull(elements);
    }

    @Override
    public @NotNull String stringify(@NotNull Precedence precedence) {
        var buffer = new StringBuilder();
        Description.DELIMITER.append(buffer, "[");
        for (var i = 0; i < this.elements.length; i++) {
            var element = this.elements[i];
            if (i > 0)
                Description.DELIMITER.append(buffer, ", ");
            buffer.append(element.stringify(Precedence.INFIX));
        }
        return Description.DELIMITER.append(buffer, "]")
                .toString();
    }
}
