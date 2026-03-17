package dev.fusemc.tau.description.dictionary.record;

import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Record(@NotNull PropertyDescription @NotNull[] properties) implements Description {

    public Record {
        Objects.requireNonNull(properties);
    }

    @Override
    public @NotNull String stringify(@NotNull Precedence precedence) {
        var buffer = new StringBuilder();
        Description.DELIMITER.wrap(buffer, '{');
        for (var i = 0; i < this.properties.length; i++) {
            var property = this.properties[i];
            if (i > 0)
                Description.DELIMITER.wrap(buffer, ", ");
            buffer.append(property.stringify(Precedence.INFIX));
        }
        return Description.DELIMITER.wrap(buffer, '}')
                .toString();
    }
}
