package dev.fusemc.tau.template.collection;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.description.Domain;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntFunction;

public record Array<T>(@NotNull Template<T> element,
                       @NotNull IntFunction<T @NotNull[]> constructor) implements Template<T[]> {

    public Array {
        Objects.requireNonNull(element);
        Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Option<T[]> lower(@NotNull Value value) {
        if (value.hasArrayElements()) {
            var length = (int) value.getArraySize();
            var buffer = this.constructor.apply(length);
            for (var i = 0; i < length; i++) {
                var element = value.getArrayElement(i);
                var option = this.element.lower(element);
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
            if (host instanceof byte[] bytes) {
                var buffer = this.constructor.apply(bytes.length);
                for (var i = 0; i < bytes.length; i++) {
                    var option = this.element.lower(Value.asValue(bytes[i]));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof short[] shorts) {
                var buffer = this.constructor.apply(shorts.length);
                for (var i = 0; i < shorts.length; i++) {
                    var option = this.element.lower(Value.asValue(shorts[i]));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof int[] ints) {
                var buffer = this.constructor.apply(ints.length);
                for (var i = 0; i < ints.length; i++) {
                    var option = this.element.lower(Value.asValue(ints[i]));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof long[] longs) {
                var buffer = this.constructor.apply(longs.length);
                for (var i = 0; i < longs.length; i++) {
                    var option = this.element.lower(Value.asValue(longs[i]));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof float[] floats) {
                var buffer = this.constructor.apply(floats.length);
                for (var i = 0; i < floats.length; i++) {
                    var option = this.element.lower(Value.asValue(floats[i]));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof double[] doubles) {
                var buffer = this.constructor.apply(doubles.length);
                for (var i = 0; i < doubles.length; i++) {
                    var option = this.element.lower(Value.asValue(doubles[i]));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof boolean[] booleans) {
                var buffer = this.constructor.apply(booleans.length);
                for (var i = 0; i < booleans.length; i++) {
                    var option = this.element.lower(Value.asValue(booleans[i]));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof char[] ints) {
                var buffer = this.constructor.apply(ints.length);
                for (var i = 0; i < ints.length; i++) {
                    var option = this.element.lower(Value.asValue(ints[i]));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof Object[] values) {
                var buffer = this.constructor.apply(values.length);
                for (var i = 0; i < values.length; i++) {
                    var option = this.element.lower(Value.asValue(values[i]));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
            if (host instanceof Collection<?> collection) {
                var buffer = this.constructor.apply(collection.size());
                var iterator = collection.iterator();
                for (var i = 0; iterator.hasNext(); i++) {
                    var element = iterator.next();
                    var option = this.element.lower(Value.asValue(element));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
                    }
                    return Option.none();
                }
                return Option.some(buffer);
            }
        }
        if (value.isProxyObject()) {
            var proxy = value.asProxyObject();
            if (proxy instanceof ProxyArray array) {
                var length = (int) array.getSize();
                var buffer = this.constructor.apply(length);
                for (var i = 0; i < length; i++) {
                    var option = this.element.lower(Value.asValue(array.get(i)));
                    if (option instanceof Option.Some<T>(var result)) {
                        buffer[i] = result;
                        continue;
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
                var option = this.element.raise(value[i]);
                if (option instanceof Option.Some<Value>(var result)) {
                    buffer[i] = result;
                    continue;
                }
                return Option.none();
            }
            return Option.some(Value.asValue(new ProxyArray() {

                @Override
                public Object get(long index) {
                    if (index >= 0 && index < buffer.length)
                        return buffer[(int) index];
                    throw new ArrayIndexOutOfBoundsException();
                }

                @Override
                public void set(long index, @NotNull Value value) {
                    Objects.requireNonNull(value);
                    throw new UnsupportedOperationException();
                }

                @Override
                public long getSize() {
                    return buffer.length;
                }
            }));
        }
        return Option.none();
    }

    /// Describes the `Array`.
    ///
    /// The returned [Description] will be annotated as having come from [Domain#TEMPLATE].
    ///
    /// An `Array` is described as follows, where `δ` denotes the `Description`
    /// of the associated `element` [Template]:
    ///
    /// ```
    /// (δ)[]
    /// ```
    ///
    /// @since `0.1.0`
    /// @see #lower(Value)
    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Description.concat(
                Description.concat(
                        Description.delimiter("("),
                        this.element.describe(points),
                        Description.delimiter(')')
                ),
                Description.delimiter("[]")
        ), Domain.TEMPLATE);
    }

    @Override
    public @NotNull String toString() {
        return Template.toString(this);
    }
}
