package dev.fusemc.tau.description.type.primitive;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public record Literal(@NotNull String literal) implements Description {

    private static final @NotNull Style STYLE = Charcoal.foreground(0x6AAB73);
    private static final @NotNull Map<Integer, String> ESCAPES = Map.of(
            (int) '\"',  "\\\"",
            (int) '\\', "\\\\",
            (int) '\n', "\\n",
            (int) '\r', "\\r",
            (int) '\t', "\\t",
            (int) '\b', "\\b",
            (int) '\f', "\\f"
    );

    public Literal {
        Objects.requireNonNull(literal);
    }

    @Override
    public @NotNull String stringify(@Nullable Domain enclosing) {
        var buffer = Literal.STYLE.begin(new StringBuilder())
                .append('"');
        for (var i = 0; i < this.literal.length();) {
            var c = this.literal.codePointAt(i);
            i += Character.charCount(c);
            if (Literal.ESCAPES.containsKey(c)) {
                Literal.STYLE.end(buffer);
                Keyword.STYLE.wrap(buffer, Literal.ESCAPES.get(c));
                Literal.STYLE.begin(buffer);
                continue;
            }
            buffer.appendCodePoint(c);
        }
        return Literal.STYLE.end(buffer.append('"'))
                .toString();
    }
}
