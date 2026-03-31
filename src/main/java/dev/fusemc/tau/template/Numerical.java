package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Template;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Numerical<N extends Number> extends Template<N> {

    @Override
    default @NotNull Option<@NotNull Value> raise(@Nullable N value) {
        if (value != null)
            return Option.some(Value.asValue(value));
        return Option.none();
    }
}
