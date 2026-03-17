package dev.fusemc.tau;

import dev.fusemc.tau.element.Accessor;
import dev.fusemc.tau.element.Element;
import dev.fusemc.tau.element.constructor.DiConstructor;
import dev.fusemc.tau.element.constructor.MonoConstructor;
import dev.fusemc.tau.element.property.Property;
import dev.fusemc.tau.template.*;
import dev.fusemc.tau.template.collection.Array;
import dev.fusemc.tau.template.collection.tuple.DiTuple;
import dev.fusemc.tau.template.collection.tuple.MonoTuple;
import dev.fusemc.tau.template.dictionary.record.DiRecord;
import dev.fusemc.tau.template.dictionary.record.MonoRecord;
import com.manchickas.optionated.Option;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Template<T> {

    @NotNull Template<@NotNull Number> NUMBER = (Numerical<Number>) value -> {
        if (value.isNumber()) {
            if (value.fitsInByte())
                return Option.some(value.asByte());
            if (value.fitsInShort())
                return Option.some(value.asShort());
            if (value.fitsInInt())
                return Option.some(value.asInt());
            if (value.fitsInLong())
                return Option.some(value.asLong());
            if (value.fitsInBigInteger())
                return Option.some(value.asBigInteger());
            if (value.fitsInFloat())
                return Option.some(value.asFloat());
            return Option.some(value.asDouble());
        }
        return Option.none();
    };
    @NotNull Template<@NotNull String> STRING = new Template<>() {

        @Override
        public @NotNull Option<@NotNull String> parse(@NotNull Value value) {
            if (value.isString())
                return Option.some(value.asString());
            return Option.none();
        }

        @Override
        public @NotNull Option<@NotNull Value> serialize(@Nullable String value) {
            if (value != null)
                return Option.some(Value.asValue(value));
            return Option.none();
        }

        @Override
        public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.STRING;
        }
    };
    @NotNull Template<@NotNull Boolean> BOOLEAN = new Template<>() {

        @Override
        public @NotNull Option<@NotNull Boolean> parse(@NotNull Value value) {
            if (value.isBoolean())
                return Option.some(value.asBoolean());
            return Option.none();
        }

        @Override
        public @NotNull Option<@NotNull Value> serialize(@Nullable Boolean value) {
            if (value != null)
                return Option.some(Value.asValue(value));
            return Option.none();
        }

        @Override
        public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.BOOLEAN;
        }
    };
    @NotNull Template<@NotNull Byte> BYTE = (Numerical<Byte>) value -> {
        if (value.isNumber() && value.fitsInByte())
            return Option.some(value.asByte());
        return Option.none();
    };
    @NotNull Template<@NotNull Short> SHORT = (Numerical<Short>) value -> {
        if (value.isNumber() && value.fitsInShort())
            return Option.some(value.asShort());
        return Option.none();
    };
    @NotNull Template<@NotNull Integer> INTEGER = (Numerical<Integer>) value -> {
        if (value.isNumber() && value.fitsInInt())
            return Option.some(value.asInt());
        return Option.none();
    };
    @NotNull Template<@NotNull Long> LONG = (Numerical<Long>) value -> {
        if (value.isNumber() && value.fitsInLong())
            return Option.some(value.asLong());
        return Option.none();
    };
    @NotNull Template<@NotNull Float> FLOAT = (Numerical<Float>) value -> {
        if (value.isNumber() && value.fitsInFloat())
            return Option.some(value.asFloat());
        return Option.none();
    };
    @NotNull Template<@NotNull Double> DOUBLE = (Numerical<Double>) value -> {
        if (value.isNumber() && value.fitsInDouble())
            return Option.some(value.asDouble());
        return Option.none();
    };
    @NotNull Template<@NotNull BigInteger> BIG_INTEGER = (Numerical<BigInteger>) value -> {
        if (value.isNumber() && value.fitsInBigInteger())
            return Option.some(value.asBigInteger());
        return Option.none();
    };
    @NotNull Template<@Nullable Void> UNDEFINED = new Template<>() {

        @Override
        public @NotNull Option<Void> parse(@NotNull Value value) {
            if (Tau.isUndefined(value))
                return Option.some(null);
            return Option.none();
        }

        @Override
        public @NotNull Option<@NotNull Value> serialize(@Nullable Void value) {
            return Option.some(Tau.undefined());
        }

        @Override
        public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.UNDEFINED;
        }
    };
    @NotNull Template<@Nullable Void> NULL = new Template<>() {

        @Override
        public @NotNull Option<@Nullable Void> parse(@NotNull Value value) {
            if (Tau.isNull(value))
                return Option.some(null);
            return Option.none();
        }

        @Override
        public @NotNull Option<@NotNull Value> serialize(@Nullable Void value) {
            return Option.some(Value.asValue(null));
        }

        @Override
        public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.NULL;
        }
    };
    @NotNull Template<@NotNull Value> ANY = new Template<>() {

        @Override
        public @NotNull Option<Value> parse(@NotNull Value value) {
            return Option.some(value);
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public @NotNull Option<@NotNull Value> serialize(@Nullable Value value) {
            if (value != null)
                return Option.some(value);
            return Option.none();
        }

        @Override
        public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.ANY;
        }
    };

    static <T> @NotNull Template<T> recursive(@NotNull Function<@NotNull Template<T>, @NotNull Template<T>> constructor) {
        Objects.requireNonNull(constructor);
        return new Mu<>(constructor);
    }

    static @NotNull Template<@NotNull String> literal(@NotNull String literal) {
        Objects.requireNonNull(literal);
        return new Literal(literal);
    }

    @SafeVarargs
    static <T> @NotNull Template<@NotNull T> union(@NotNull Template<T> @NotNull... alternatives) {
        Objects.requireNonNull(alternatives);
        return new Union<>(Arrays.copyOf(alternatives, alternatives.length));
    }

    static <T> @NotNull Template<@NotNull T> reference(@NotNull Class<T> type) {
        Objects.requireNonNull(type);
        return new Reference<>(type);
    }

    static <T> @NotNull Template<@NotNull T[]> array(@NotNull Template<T> element,
                                                     @NotNull IntFunction<T @NotNull[]> constructor) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(constructor);
        return new Array<>(element, constructor);
    }

    static <T, A> @NotNull Element<T, A> element(@NotNull Template<A> template,
                                                 @NotNull Accessor<T, A> accessor) {
        Objects.requireNonNull(template);
        Objects.requireNonNull(accessor);
        return new Element<>(template, accessor);
    }

    static <T, A> @NotNull Template<T> tuple(@NotNull Element<T, A> a,
                                             @NotNull MonoConstructor<T, A> constructor) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(constructor);
        return new MonoTuple<>(a, constructor);
    }

    static <T, A, B> @NotNull Template<T> tuple(@NotNull Element<T, A> a,
                                                @NotNull Element<T, B> b,
                                                @NotNull DiConstructor<T, A, B> constructor) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(constructor);
        return new DiTuple<>(a, b, constructor);
    }

    static <T, A> Property.@NotNull Optional<T, A> optional(@NotNull Property.Required<T, A> property,
                                                            @NotNull Supplier<A> supplier) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(supplier);
        return new Property.Optional<>(property, supplier);
    }

    static <T, A> Property.@NotNull Required<T, A> property(@NotNull String name,
                                                   @NotNull Template<A> template,
                                                   @NotNull Accessor<T, A> accessor) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(template);
        Objects.requireNonNull(accessor);
        return new Property.Required<>(name, template, accessor);
    }

    static <T, A> @NotNull Template<T> record(@NotNull Property<T, A> a,
                                              @NotNull MonoConstructor<T, A> constructor) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(constructor);
        return new MonoRecord<>(a, constructor);
    }

    static <T, A, B> @NotNull Template<T> record(@NotNull Property<T, A> a,
                                                 @NotNull Property<T, B> b,
                                                 @NotNull DiConstructor<T, A, B> constructor) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(constructor);
        return new DiRecord<>(a, b, constructor);
    }

    default <V> @NotNull Template<V> map(@NotNull Function<T, V> forward,
                                         @NotNull Function<V, T> backward) {
        Objects.requireNonNull(forward);
        Objects.requireNonNull(backward);
        return new Template<>() {

            @Override
            public @NotNull Option<V> parse(@NotNull Value value) {
                return Template.this.parse(value)
                        .map(forward);
            }

            @Override
            public @NotNull Option<@NotNull Value> serialize(@Nullable V value) {
                return Template.this.serialize(backward.apply(value));
            }

            @Override
            public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
                return Template.this.description(points);
            }
        };
    }

    default <V> @NotNull Template<V> flatMap(@NotNull Function<T, @NotNull Option<V>> forward,
                                             @NotNull Function<@Nullable V, @NotNull Option<T>> backward) {
        Objects.requireNonNull(forward);
        Objects.requireNonNull(backward);
        return new Template<>() {

            @Override
            public @NotNull Option<V> parse(@NotNull Value value) {
                return Template.this.parse(value)
                        .flatMap(forward);
            }

            @Override
            public @NotNull Option<@NotNull Value> serialize(@Nullable V value) {
                return backward.apply(value)
                        .flatMap(Template.this::serialize);
            }

            @Override
            public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
                return Template.this.description(points);
            }
        };
    }

    default @NotNull Template<T> filter(@NotNull Predicate<T> forward,
                                        @NotNull Predicate<@Nullable T> backward) {
        Objects.requireNonNull(forward);
        Objects.requireNonNull(backward);
        return new Template<>() {

            @Override
            public @NotNull Option<T> parse(@NotNull Value value) {
                return Template.this.parse(value)
                        .filter(forward);
            }

            @Override
            public @NotNull Option<@NotNull Value> serialize(@Nullable T value) {
                if (backward.test(value))
                    return Template.this.serialize(value);
                return Option.none();
            }

            @Override
            public @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points) {
                return Template.this.description(points);
            }
        };
    }

    @NotNull Option<T> parse(@NotNull Value value);

    @NotNull Option<@NotNull Value> serialize(@Nullable T value);

    default Description description() {
        return this.description(Scope.hashScope());
    }

    @NotNull Description description(@NotNull Scope<@NotNull Mu<?>> points);
}
