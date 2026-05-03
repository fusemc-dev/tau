package dev.fusemc.tau.proxy;

import dev.fusemc.tau.Tau;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

/// Represents an object-like.
///
/// An `ObjectLike` is a specific implementation of a [ProxyObject]
/// that acts as an immutable, read-only hash table after its creation.
/// It is intended as a way to expose object-like objects to scripts.
///
/// An `ObjectLike` is initiated with a [Builder].
///
/// @since `0.1.0`
/// @see Builder
public final class ObjectLike implements ProxyObject {

    private final Entry @NotNull[] table;
    private final Entry @NotNull[] insertion;

    private ObjectLike(Entry @NotNull[] table,
                       Entry @NotNull[] insertion) {
        this.table = Objects.requireNonNull(table);
        this.insertion = Objects.requireNonNull(insertion);
    }

    /// Constructs an [Builder].
    ///
    /// Initializes a `Builder` instance with the default capacity of 16.
    ///
    /// @since `0.1.0`
    public static @NotNull Builder builder() {
        return new Builder(16);
    }

    /// Constructs an [Builder].
    ///
    /// Initializes a `Builder` instance with the provided initial `capacity`.
    ///
    /// @since `0.1.0`
    public static @NotNull Builder builder(int capacity) {
        return new Builder(capacity);
    }

    public static <T> @NotNull Collector<T, Builder, ObjectLike> toObject(@NotNull Function<T, String> key,
                                                                          @NotNull Function<T, Value> value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return Collector.of(
                ObjectLike::builder,
                (builder, entry) -> builder.append(key.apply(entry), value.apply(entry)),
                Builder::appendAll,
                Builder::build
        );
    }

    @ApiStatus.Internal
    private static int hash(@NotNull String key) {
        var h = 0x811c9dc5;
        for (var i = 0; i < key.length(); i++)
            h = (h ^ key.charAt(i)) * 0x01000193;
        return h;
    }

    /// Retrieves the [Value] associated with the provided `key`.
    ///
    /// If no mapping for the given `key` was provided when building the `ObjectLike`,
    /// [Tau#undefined()] is returned instead.
    ///
    /// @since `0.1.0`
    /// @see Tau#undefined()
    /// @see #hasMember(String)
    @Override
    public @NotNull Value getMember(@NotNull String key) {
        Objects.requireNonNull(key);
        var h = ObjectLike.hash(key) & (this.table.length - 1);
        for (int i = h, j = 0; j < this.table.length; i = (i + 1) & (this.table.length - 1), j++) {
            var entry = this.table[i];
            if (entry != null) {
                if (entry.isOf(key))
                    return entry.value;
                continue;
            }
            return Tau.undefined();
        }
        return Tau.undefined();
    }

    /// Returns a view of the keys in the `ObjectLike`.
    ///
    /// The returned [ProxyArray] is an **immutable** view of the keys in the `ObjectLike`,
    /// in the order they were inserted when building it.
    ///
    /// @since `0.1.0`
    @Override
    public @NotNull ProxyArray getMemberKeys() {
        return new ProxyArray() {

            @Override
            public Object get(long index) {
                if (index >= 0 && index < ObjectLike.this.insertion.length)
                    return ObjectLike.this.insertion[(int) index].key;
                throw new ArrayIndexOutOfBoundsException();
            }

            @Override
            public void set(long index, Value value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getSize() {
                return ObjectLike.this.insertion.length;
            }
        };
    }

    /// Determines whether the `ObjectLike` contains a mapping associated with the provided `key`.
    ///
    /// @since `0.1.0`
    /// @see #getMember(String)
    @Override
    public boolean hasMember(@NotNull String key) {
        Objects.requireNonNull(key);
        var h = ObjectLike.hash(key) & (this.table.length - 1);
        for (int i = h, j = 0; j < this.table.length; i = (i + 1) & (this.table.length - 1), j++) {
            var entry = this.table[i];
            if (entry != null) {
                if (entry.isOf(key))
                    return true;
                continue;
            }
            return false;
        }
        return false;
    }

    /// Attempts to insert the provided mapping to the `ObjectLike`.
    ///
    /// Since `Dictionaries` are immutable after their construction,
    /// this method always throws an [UnsupportedOperationException].
    ///
    /// @since `0.1.0`
    @Override
    public void putMember(@NotNull String key, @NotNull Value value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        throw new UnsupportedOperationException("putMember()");
    }

    /// Returns the string representation of the `ObjectLike`.
    ///
    /// Since `ObjectLike`s attempt to mimic standard JavaScript objects,
    /// the string representation is always `[object Object']`, similar
    /// to the standard `Object.toString()` method.
    ///
    /// @since `0.1.0`
    @Override
    public String toString() {
        return "[object Object']";
    }

    /// Represents a mutable `ObjectLike` Builder.
    ///
    /// A `Builder` lets one assemble a `ObjectLike` incrementally.
    /// The order in which the entries were `appended` is preserved
    /// when `building` the `ObjectLike`.
    /// 
    /// @since `0.1.0`
    /// @see ObjectLike#builder() 
    /// @see ObjectLike#builder(int)
    public static class Builder {

        private static final float LOAD_FACTOR = 0.75f;

        private Entry @NotNull[] table;
        private Entry @NotNull[] insertion;
        private int length;

        private Builder(int capacity) {
            this.table     = new ObjectLike.Entry[Builder.normalizeCapacity(capacity)];
            this.insertion = new ObjectLike.Entry[this.table.length];
            this.length    = 0;
        }

        @ApiStatus.Internal
        private static int normalizeCapacity(int n) {
            if (n > 0)
                return Integer.highestOneBit(n - 1) << 1;
            return 0;
        }

        /// Appends the provided mapping to the `Builder`.
        ///
        /// Associates the given `key` with the provided `value` in the `Builder`. If the `key`
        /// was already present, an [IllegalArgumentException] is thrown.
        ///
        /// @since `0.1.0`
        /// @see #build()
        @Contract("_, _ -> this")
        public @NotNull Builder append(@NotNull String key, @NotNull Value value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            if ((this.length + 1) >= this.table.length * Builder.LOAD_FACTOR)
                this.table = this.rehash(this.table.length << 1);
            var h = ObjectLike.hash(key) & (this.table.length - 1);
            for (int i = h ;; i = (i + 1) & (this.table.length - 1)) {
                var entry = this.table[i];
                if (entry == null) {
                    this.table[i] = new ObjectLike.Entry(key, value);
                    if (this.length >= this.insertion.length)
                        this.insertion = Arrays.copyOf(this.insertion, this.insertion.length << 1);
                    this.insertion[this.length++] = this.table[i];
                    return this;
                }
                if (entry.isOf(key))
                    throw new IllegalArgumentException();
            }
        }

        @Contract("_ -> this")
        public @NotNull Builder appendAll(@NotNull Builder builder) {
            Objects.requireNonNull(builder);
            for (var entry : builder.insertion) {
                if (entry != null)
                    this.append(entry.key, entry.value);
            }
            return this;
        }

        /// Assembles the `ObjectLike`.
        ///
        /// Assembles the final `ObjectLike` from the mappings that were
        /// previously `appended` to the `Builder`.
        ///
        /// The `build()` method may be called multiple times on the same `Builder`.
        ///
        /// @since `0.1.0`
        /// @see #append(String, Value)
        public @NotNull ObjectLike build() {
            return new ObjectLike(
                    Arrays.copyOf(this.table, this.table.length),
                    Arrays.copyOf(this.insertion, this.length)
            );
        }

        @ApiStatus.Internal
        private Entry @NotNull[] rehash(int length) {
            var buffer = new Entry[length];
            for (var entry : this.table) {
                if (entry != null) {
                    var h = ObjectLike.hash(entry.key) & (buffer.length - 1);
                    for (int i = h ;; i = (i + 1) & (buffer.length - 1)) {
                        if (buffer[i] != null)
                            continue;
                        buffer[i] = entry;
                        break;
                    }
                }
            }
            return buffer;
        }
    }

    @ApiStatus.Internal
    private record Entry(@NotNull String key, Value value) {

        public Entry {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }

        public boolean isOf(@NotNull String key) {
            Objects.requireNonNull(key);
            return this.key.equals(key);
        }
    }
}
