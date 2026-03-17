package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Numerical<N extends Number> extends Template<N> {

    @Override
    default @NotNull Option<@NotNull Value> serialize(@Nullable N value) {
        if (value != null)
            return Option.some(Value.asValue(value));
        return Option.none();
    }

    @Override
    default @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.NUMBER;
    }
}
