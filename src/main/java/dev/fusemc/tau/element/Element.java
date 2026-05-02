package dev.fusemc.tau.element;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
                if (host instanceof byte[] bytes) {
                    if (position < bytes.length)
                        return this.template.lower(Value.asValue(bytes[position]));
                    return Option.none();
                }
                if (host instanceof short[] shorts) {
                    if (position < shorts.length)
                        return this.template.lower(Value.asValue(shorts[position]));
                    return Option.none();
                }
                if (host instanceof int[] ints) {
                    if (position < ints.length)
                        return this.template.lower(Value.asValue(ints[position]));
                    return Option.none();
                }
                if (host instanceof long[] longs) {
                    if (position < longs.length)
                        return this.template.lower(Value.asValue(longs[position]));
                    return Option.none();
                }
                if (host instanceof float[] floats) {
                    if (position < floats.length)
                        return this.template.lower(Value.asValue(floats[position]));
                    return Option.none();
                }
                if (host instanceof double[] doubles) {
                    if (position < doubles.length)
                        return this.template.lower(Value.asValue(doubles[position]));
                    return Option.none();
                }
                if (host instanceof boolean[] booleans) {
                    if (position < booleans.length)
                        return this.template.lower(Value.asValue(booleans[position]));
                    return Option.none();
                }
                if (host instanceof char[] chars) {
                    if (position < chars.length)
                        return this.template.lower(Value.asValue(chars[position]));
                    return Option.none();
                }
                if (host instanceof Object[] objects) {
                    if (position < objects.length)
                        return this.template.lower(Value.asValue(objects[position]));
                    return Option.none();
                }
                if (host instanceof Map.Entry<?, ?> entry)
                    return switch (position) {
                        case 0 -> this.template.lower(Value.asValue(entry.getKey()));
                        case 1 -> this.template.lower(Value.asValue(entry.getValue()));
                        default -> Option.none();
                    };
                if (host instanceof List<?> list) {
                    var length = list.size();
                    if (position < length)
                        return this.template.lower(Value.asValue(list.get(position)));
                    return Option.none();
                }
                return Option.none();
            }
            if (value.isProxyObject()) {
                var proxy = value.asProxyObject();
                if (proxy instanceof ProxyArray array) {
                    var length = (int) array.getSize();
                    if (position < length)
                        return this.template.lower(Value.asValue(array.get(position)));
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
