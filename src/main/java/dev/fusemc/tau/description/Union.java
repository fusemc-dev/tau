package dev.fusemc.tau.description;

import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;

public record Union(@NotNull Description @NotNull[] alternatives) implements Description {

    public Union {
        Objects.requireNonNull(alternatives);
    }

    @Override
    public @NotNull String stringify(@NotNull Precedence precedence) {
        var buffer = new StringBuilder();
        var visited = new HashSet<@NotNull Description>();
        if (this.shouldParenthesize(precedence))
            Description.DELIMITER.wrap(buffer, "(");
        for (int i = 0, j = 0; i < this.alternatives.length; i++) {
            var alternative = this.alternatives[i];
            if (visited.add(alternative)) {
                if (j++ > 0)
                    Description.DELIMITER.wrap(buffer, " | ");
                buffer.append(alternative.stringify(Precedence.INFIX));
            }
        }
        if (this.shouldParenthesize(precedence))
            Description.DELIMITER.wrap(buffer, ")");
        return buffer.toString();
    }

    private boolean shouldParenthesize(@NotNull Precedence precedence) {
        Objects.requireNonNull(precedence);
        return precedence.isTighterThan(Precedence.INFIX) && this.alternatives.length > 1;
    }
}
