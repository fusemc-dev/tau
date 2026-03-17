package dev.fusemc.tau.description;

import com.manchickas.charcoal.Charcoal;
import com.manchickas.charcoal.Style;
import dev.fusemc.tau.Description;
import dev.fusemc.tau.Documented;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public record Reference(@NotNull Class<?> type) implements Description {

    private static final @NotNull Style STYLE = Charcoal.foreground(0x56A8F5);

    public Reference {
        Objects.requireNonNull(type);
    }

    @Override
    public @NotNull String stringify(@NotNull Precedence precedence) {
        var metadata = this.type.getAnnotation(Documented.class);
        if (metadata != null) {
            var type = metadata.value();
            var ref = metadata.reference();
            if (ref.isBlank())
                return Reference.STYLE.wrap(type);
            try {
                var uri = new URI(ref);
                return Reference.STYLE.link(uri, type);
            } catch (URISyntaxException e) {
                return Reference.STYLE.wrap(type);
            }
        }
        return Reference.STYLE.wrap(this.type.getSimpleName());
    }
}
