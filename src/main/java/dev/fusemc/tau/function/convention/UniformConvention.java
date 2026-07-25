package dev.fusemc.tau.function.convention;

import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public interface UniformConvention<T> extends Convention<T> {

    @Override
    default @NotNull Value callVariadic(@NotNull T callee, Object @Nullable [] args) {
        if (args != null) {
            var variadic = UniformConvention.box(args[args.length - 1]);
            var buffer   = Arrays.copyOf(args, args.length + variadic.length - 1);
            System.arraycopy(variadic, 0, buffer, args.length - 1, variadic.length);
            return this.call(callee, buffer);
        }
        return this.call(callee, null);
    }

    /// Box the variadic arguments.
    ///
    /// ---
    ///
    /// Converts an array of (potentially primitive) values into a boxed array of the corresponding type.
    /// If the given `variadic` does not represent an array, an `AssertionError` is thrown.
    ///
    /// ```
    /// new int[]{42, 67, 1337} -> new Integer[]{42, 67, 1337}
    /// ```
    ///
    ///
    /// @since 0.3.0
    static @NotNull Object[] box(@NotNull Object variadic) {
        Objects.requireNonNull(variadic);
        return switch (variadic) {
            case byte[] bytes -> {
                var buffer = new Byte[bytes.length];
                for (var i = 0; i < bytes.length; i++)
                    buffer[i] = bytes[i];
                yield buffer;
            }
            case short[] shorts -> {
                var buffer = new Short[shorts.length];
                for (var i = 0; i < shorts.length; i++)
                    buffer[i] = shorts[i];
                yield buffer;
            }
            case int[] ints -> {
                var buffer = new Integer[ints.length];
                for (var i = 0; i < ints.length; i++)
                    buffer[i] = ints[i];
                yield buffer;
            }
            case long[] longs -> {
                var buffer = new Long[longs.length];
                for (var i = 0; i < longs.length; i++)
                    buffer[i] = longs[i];
                yield buffer;
            }
            case float[] floats -> {
                var buffer = new Float[floats.length];
                for (var i = 0; i < floats.length; i++)
                    buffer[i] = floats[i];
                yield buffer;
            }
            case double[] doubles -> {
                var buffer = new Double[doubles.length];
                for (var i = 0; i < doubles.length; i++)
                    buffer[i] = doubles[i];
                yield buffer;
            }
            case boolean[] booleans -> {
                var buffer = new Boolean[booleans.length];
                for (var i = 0; i < booleans.length; i++)
                    buffer[i] = booleans[i];
                yield buffer;
            }
            case char[] chars -> {
                var buffer = new Character[chars.length];
                for (var i = 0; i < chars.length; i++)
                    buffer[i] = chars[i];
                yield buffer;
            }
            case Object[] objects -> objects;
            default -> throw new AssertionError();
        };
    }
}
