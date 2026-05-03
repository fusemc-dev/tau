package dev.fusemc.tau.proxy;

import dev.fusemc.tau.Tau;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.graalvm.polyglot.proxy.ProxyIterator;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

/// Represents a map-like.
///
/// A `MapLike` is a specific implementation of a [ProxyHashMap]
/// that acts as an immutable, read-only hash map after its creation.
/// It is intended as a way to expose map-like objects to scripts.
///
/// A `MapLike` is initiated with a [MapLike.Builder].
///
/// @since `0.1.0`
/// @see Builder
public final class MapLike implements ProxyHashMap {

    private final Entry @NotNull[] table;
    private final Entry @NotNull[] insertion;

    private MapLike(Entry @NotNull[] table,
                    Entry @NotNull[] insertion) {
        this.table     = Objects.requireNonNull(table);
        this.insertion = Objects.requireNonNull(insertion);
    }

    public static @NotNull Builder builder() {
        return new Builder(16);
    }

    public static @NotNull Builder builder(int capacity) {
        return new Builder(capacity);
    }

    public static <T> @NotNull Collector<T, MapLike.Builder, MapLike> toMap(@NotNull Function<T, Value> key,
                                                                            @NotNull Function<T, Value> value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return Collector.of(
                MapLike::builder,
                (builder, entry) -> builder.append(key.apply(entry), value.apply(entry)),
                Builder::appendAll,
                Builder::build
        );
    }

    @ApiStatus.Internal
    private static int spread(int h) {
        h ^= h >> 16;
        h *= 0x85ebca6b;
        h ^= h >> 13;
        h *= 0xc2b2ae35;
        h ^= h >> 16;
        return h;
    }

    @Override
    public @NotNull Value getHashValue(@NotNull Value key) {
        Objects.requireNonNull(key);
        var h = MapLike.spread(key.hashCode()) & (this.table.length - 1);
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

    @Override
    public boolean hasHashEntry(@NotNull Value key) {
        Objects.requireNonNull(key);
        var h = MapLike.spread(key.hashCode()) & (this.table.length - 1);
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

    @Override
    public void putHashEntry(@NotNull Value key, @NotNull Value value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ProxyIterator getHashEntriesIterator() {
        return new ProxyIterator() {

            private int position = 0;

            @Override
            public @NotNull ProxyArray getNext() throws NoSuchElementException {
                if (this.position < MapLike.this.insertion.length)
                    return MapLike.this.insertion[this.position++];
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasNext() {
                return this.position < MapLike.this.insertion.length;
            }
        };
    }

    @Override
    public long getHashSize() {
        return this.insertion.length;
    }

    @Override
    public String toString() {
        return "[object Map']";
    }

    public static final class Builder {

        private static final float LOAD_FACTOR = 0.75f;

        private Entry @NotNull[] table;
        private Entry @NotNull[] insertion;
        private int length;

        public Builder(int capacity) {
            this.table     = new Entry[Builder.normalizeCapacity(capacity)];
            this.insertion = new Entry[this.table.length];
            this.length    = 0;
        }

        @ApiStatus.Internal
        private static int normalizeCapacity(int n) {
            if (n > 0)
                return Integer.highestOneBit(n - 1) << 1;
            return 0;
        }

        @Contract("_, _ -> this")
        public @NotNull Builder append(@NotNull Value key, @NotNull Value value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            if ((this.length + 1) >= this.table.length * Builder.LOAD_FACTOR)
                this.table = this.rehash(this.table.length << 1);
            var h = MapLike.spread(key.hashCode()) & (this.table.length - 1);
            for (var i = h ;; i = (i + 1) & (this.table.length - 1)) {
                var entry = this.table[i];
                if (entry == null) {
                    this.table[i] = new Entry(key, value);
                    if (this.length >= this.insertion.length)
                        this.insertion = Arrays.copyOf(this.insertion, this.insertion.length << 1);
                    this.insertion[this.length++] = this.table[i];
                    return this;
                }
                if (entry.isOf(key))
                    throw new IllegalArgumentException();
            }
        }

        public @NotNull Builder appendAll(@NotNull Builder builder) {
            Objects.requireNonNull(builder);
            for (var entry : builder.insertion) {
                if (entry != null)
                    this.append(entry.key, entry.value);
            }
            return this;
        }

        public @NotNull MapLike build() {
            return new MapLike(
                    Arrays.copyOf(this.table, this.table.length),
                    Arrays.copyOf(this.insertion, this.length)
            );
        }

        @ApiStatus.Internal
        private Entry @NotNull[] rehash(int length) {
            var buffer = new Entry[length];
            for (var entry : this.table) {
                if (entry != null) {
                    var h = MapLike.spread(entry.key.hashCode()) & (buffer.length - 1);
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
    private record Entry(@NotNull Value key, @NotNull Value value) implements ProxyArray {

        public Entry {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }

        public boolean isOf(@NotNull Value key) {
            Objects.requireNonNull(key);
            return this.key.equals(key);
        }

        @Override
        public @NotNull Value get(long index) {
            return switch ((int) index) {
                case 0 -> this.key;
                case 1 -> this.value;
                default -> throw new ArrayIndexOutOfBoundsException();
            };
        }

        @Override
        public void set(long index, @NotNull Value value) {
            Objects.requireNonNull(value);
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            return 2;
        }
    }
}
