package dev.fusemc.tau.template.collection;

import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.function.IntFunction;

public record Array<T>(@NotNull Template<T> template,
                       @NotNull IntFunction<T @NotNull[]> constructor) implements Template<T[]> {

    public Array {
        Objects.requireNonNull(template);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T[]> lower(@NotNull Value value) {
        if (value.hasArrayElements()) {
            var length = (int) value.getArraySize();
            var buffer = this.constructor.apply(length);
            for (var i = 0; i < length; i++) {
                var element = value.getArrayElement(i);
                var option = this.template.lower(element);
                if (option instanceof Option.Some<T>(var result)) {
                    buffer[i] = result;
                    continue;
                }
                return Option.none();
            }
            return Option.some(buffer);
        }
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (host instanceof Value[] values) {
                var buffer = this.constructor.apply(values.length);
                for (var i = 0; i < values.length; i++) {
                    var option = this.template.lower(values[i]);
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof Collection<?> collection) {
                var length = collection.size();
                var buffer = this.constructor.apply(length);
                var iterator = collection.iterator();
                for (var i = 0; iterator.hasNext(); i++) {
                    var element = iterator.next();
                    if (element instanceof Value v) {
                        var option = this.template.lower(v);
                        if (option instanceof Option.Some<T>(var result)) {
                            buffer[i] = result;
                            continue;
                        }
                        return Option.none();
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            return Option.none();
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T[] value) {
        if (value != null) {
            var buffer = new Value[value.length];
            for (var i = 0; i < value.length; i++) {
                var option = this.template.raise(value[i]);
                if (option instanceof Option.Some<Value>(var result)) {
                    buffer[i] = result;
                    continue;
                }
                return Option.none();
            }
            return Option.some(Value.asValue(buffer));
        }
        return Option.none();
    }

    @Override
    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.concat(
                Description.concat(
                        Description.delimiter("("),
                        this.template.description(points),
                        Description.delimiter(')')
                ),
                Description.delimiter("[]")
        );
    }
}
