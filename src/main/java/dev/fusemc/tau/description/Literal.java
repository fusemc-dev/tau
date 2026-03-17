package dev.fusemc.tau.description;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public record Literal(@NotNull String literal) implements Description {

    public static final @NotNull Style STYLE = Charcoal.foreground(0x6AAB73);
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
    public @NotNull String stringify(@NotNull Precedence precedence) {
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
        buffer.append('"');
        return Literal.STYLE.end(buffer).toString();
    }
}
