package dev.fusemc.tau.template.collection.tuple;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Domain;
import dev.fusemc.tau.element.Element;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface Tuple<T> extends Template<T> {

    @ApiStatus.Internal
    static boolean isTuple(@NotNull Value value, int length) {
        Objects.requireNonNull(value);
        if (value.hasArrayElements())
            return length == value.getArraySize();
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (host instanceof byte[] bytes)
                return length == bytes.length;
            if (host instanceof short[] shorts)
                return length == shorts.length;
            if (host instanceof int[] ints)
                return length == ints.length;
            if (host instanceof long[] longs)
                return length == longs.length;
            if (host instanceof float[] floats)
                return length == floats.length;
            if (host instanceof double[] doubles)
                return length == doubles.length;
            if (host instanceof boolean[] booleans)
                return length == booleans.length;
            if (host instanceof char[] chars)
                return length == chars.length;
            if (host instanceof Object[] objects)
                return length == objects.length;
            if (host instanceof Map.Entry<?, ?>)
                return length == 2;
            if (host instanceof List<?> list)
                return length == list.size();
            return false;
        }
        if (value.isProxyObject()) {
            var proxy = value.asProxyObject();
            if (proxy instanceof ProxyArray array)
                return length == (int) array.getSize();
            return false;
        }
        return false;
    }

    @SafeVarargs
    static <T> @NotNull Option<Value> serialize(@NotNull T instance,
                                                @NotNull Element<T, ?> @NotNull... elements) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(elements);
        var buffer = new Value[elements.length];
        for (var i = 0; i < elements.length; i++) {
            var element = Objects.requireNonNull(elements[i]);
            var option = element.raise(instance);
            if (option instanceof Option.Some<Value>(var result)) {
                buffer[i] = result;
                continue;
            }
            return Option.none();
        }
        return Option.some(Value.asValue(buffer));
    }

    static @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points,
                                         @NotNull Element<?, ?> @NotNull... elements) {
        Objects.requireNonNull(elements);
        Objects.requireNonNull(points);
        return Description.concat(
                Description.delimiter('['),
                Description.join(Description.delimiter(", "), Arrays.stream(elements)
                        .map(el -> el.description(points))
                        .toArray(Description[]::new)),
                Description.delimiter(']')
        );
    }

    /// Describes the `Tuple`.
    ///
    /// The returned [Description] will be annotated as having come from [Domain#TEMPLATE].
    ///
    /// A `Tuple` is described as follows, where `δₙ` denotes the `Description`
    /// of the n-th [Template] of the tuple:
    ///
    /// ```
    /// [δ₀, δ₁, ..., δₙ]
    /// ```
    ///
    /// @since `0.1.0`
    /// @see #lower(Value)
    /// @see #raise(Object) 
    @Override
    @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points);
}
