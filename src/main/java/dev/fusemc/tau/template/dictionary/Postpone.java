package dev.fusemc.tau.template.dictionary;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.element.Property;
import dev.fusemc.tau.proxy.ObjectLike;
import dev.fusemc.tau.template.Mu;
import dev.fusemc.tau.template.dictionary.record.Record;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Postpone<T>(@NotNull Template<T> delegate) implements Record<T> {

    @Override
    public @NotNull Option<T> lower(@NotNull Value value) {
        Objects.requireNonNull(value);
        return this.delegate.lower(value);
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        return this.delegate.raise(value);
    }

    @Override
    public @NotNull Option<Value> raiseWith(@Nullable T instance, @NotNull Property<? super T, ?> property) {
        if (instance == null)
            return Option.none();
        return this.delegate.raise(instance)
                .flatMap(m -> Template.map(Template.STRING, Template.ANY).lower(m))
                .flatMap(map -> property.raise(instance)
                        .map(value -> {
                            var buffer = ObjectLike.builder(map.size());
                            for (var entry : map.entrySet())
                                buffer.append(entry.getKey(), entry.getValue());
                            buffer.append(property.name(), value);
                            return buffer.build();
                        }))
                .map(Value::asValue);
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        Objects.requireNonNull(points);
        return this.delegate.describe(points);
    }
}
