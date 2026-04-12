package dev.fusemc.tau.element;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

/// An `Element` represents a single element of a tuple.
///
/// An `Element` associates a single element of a tuple with
/// a property of type `A` on type `T`. The [Accessor] function
/// specifies how the property should be accessed from an instance.
///
/// @see Property
public record Element<T, A>(@NotNull Template<A> template,
                            @NotNull Accessor<T, A> accessor) {

    public Element {
        Objects.requireNonNull(template);
        Objects.requireNonNull(accessor);
    }

    public @NotNull Option<A> lower(@NotNull Value value, int position) {
        Objects.requireNonNull(value);
        if (position >= 0) {
            if (value.hasArrayElements()) {
                var length = (int) value.getArraySize();
                if (position < length)
                    return this.template.lower(value.getArrayElement(position));
                return Option.none();
            }
            if (value.isHostObject()) {
                var host = value.asHostObject();
                if (host instanceof Value[] values) {
                    if (position < values.length)
                        return this.template.lower(values[position]);
                    return Option.none();
                }
                if (host instanceof Collection<?> collection) {
                    var iterator = collection.iterator();
                    for (int i = 0; i < position; i++) {
                        if (iterator.hasNext()) {
                            iterator.next();
                            continue;
                        }
                        return Option.none();
                    }
                    if (iterator.hasNext()) {
                        var element = iterator.next();
                        if (element instanceof Value v)
                            return this.template.lower(v);
                        return Option.none();
                    }
                    return Option.none();
                }
                return Option.none();
            }
            return Option.none();
        }
        return Option.none();
    }

    public @NotNull Option<Value> raise(@NotNull T instance) {
        Objects.requireNonNull(instance);
        return this.template.raise(this.accessor.access(instance));
    }

    public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
        return this.template.describe(points);
    }
}
