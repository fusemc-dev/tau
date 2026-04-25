package dev.fusemc.tau;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import dev.fusemc.tau.element.Accessor;
import dev.fusemc.tau.element.Element;
import dev.fusemc.tau.element.constructor.*;
import dev.fusemc.tau.element.Property;
import dev.fusemc.tau.template.*;
import dev.fusemc.tau.template.collection.Array;
import dev.fusemc.tau.template.collection.tuple.DiTuple;
import dev.fusemc.tau.template.collection.tuple.MonoTuple;
import dev.fusemc.tau.template.dictionary.Dispatch;
import dev.fusemc.tau.template.dictionary.record.*;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.dictionary.record.Record;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public interface Template<T> {

    @NotNull Template<@NotNull Number> NUMBER = new Numerical<>() {

        @Override
        public @NotNull Option<Number> lower(@NotNull Value value) {
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
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.NUMBER, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull String> STRING = new Template<>() {

        @Override
        public @NotNull Option<@NotNull String> lower(@NotNull Value value) {
            if (value.isString())
                return Option.some(value.asString());
            return Option.none();
        }

        @Override
        public @NotNull Option<@NotNull Value> raise(@Nullable String value) {
            if (value != null)
                return Option.some(Value.asValue(value));
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.STRING, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull Boolean> BOOLEAN = new Template<>() {

        @Override
        public @NotNull Option<@NotNull Boolean> lower(@NotNull Value value) {
            if (value.isBoolean())
                return Option.some(value.asBoolean());
            return Option.none();
        }

        @Override
        public @NotNull Option<@NotNull Value> raise(@Nullable Boolean value) {
            if (value != null)
                return Option.some(Value.asValue(value));
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.BOOLEAN, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull Byte> BYTE = new Numerical<>() {

        @Override
        public @NotNull Option<Byte> lower(@NotNull Value value) {
            if (value.isNumber() && value.fitsInByte())
                return Option.some(value.asByte());
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.BYTE, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull Short> SHORT = new Numerical<>() {

        @Override
        public @NotNull Option<Short> lower(@NotNull Value value) {
            if (value.isNumber() && value.fitsInShort())
                return Option.some(value.asShort());
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.SHORT, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull Integer> INTEGER = new Numerical<>() {

        @Override
        public @NotNull Option<@NotNull Integer> lower(@NotNull Value value) {
            if (value.isNumber() && value.fitsInInt())
                return Option.some(value.asInt());
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.INTEGER, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull Long> LONG = new Numerical<>() {

        @Override
        public @NotNull Option<Long> lower(@NotNull Value value) {
            if (value.isNumber() && value.fitsInLong())
                return Option.some(value.asLong());
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.LONG, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull Float> FLOAT = new Numerical<>() {

        @Override
        public @NotNull Option<Float> lower(@NotNull Value value) {
            if (value.isNumber() && value.fitsInFloat())
                return Option.some(value.asFloat());
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.FLOAT, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull Double> DOUBLE = new Numerical<>() {

        @Override
        public @NotNull Option<Double> lower(@NotNull Value value) {
            if (value.isNumber() && value.fitsInDouble())
                return Option.some(value.asDouble());
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.DOUBLE, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull BigInteger> BIG_INTEGER = new Numerical<>() {

        @Override
        public @NotNull Option<BigInteger> lower(@NotNull Value value) {
            if (value.isNumber() && value.fitsInBigInteger())
                return Option.some(value.asBigInteger());
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.BIG_INTEGER, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@Nullable Void> UNDEFINED = new Template<>() {

        @Override
        public @NotNull Option<Void> lower(@NotNull Value value) {
            if (Tau.isUndefined(value))
                return Option.some(null);
            return Option.none();
        }

        @Override
        public @NotNull Option<@NotNull Value> raise(@Nullable Void value) {
            return Option.some(Tau.undefined());
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.UNDEFINED, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@Nullable Void> NULL = new Template<>() {

        @Override
        public @NotNull Option<@Nullable Void> lower(@NotNull Value value) {
            if (Tau.isNull(value))
                return Option.some(null);
            return Option.none();
        }

        @Override
        public @NotNull Option<@NotNull Value> raise(@Nullable Void value) {
            return Option.some(Value.asValue(null));
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.NULL, Domain.TEMPLATE);
        }
    };
    @NotNull Template<@NotNull Value> ANY = new Template<>() {

        @Override
        public @NotNull Option<Value> lower(@NotNull Value value) {
            return Option.some(value);
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public @NotNull Option<@NotNull Value> raise(@Nullable Value value) {
            if (value != null)
                return Option.some(value);
            return Option.none();
        }

        @Override
        public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
            return Description.attach(Description.ANY, Domain.TEMPLATE);
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

    static <T> @NotNull Template<T @NotNull[]> array(@NotNull Template<T> element,
                                                     @NotNull IntFunction<T @NotNull[]> constructor) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(constructor);
        return new Array<>(element, constructor);
    }

    static <T, A> @NotNull Template<@NotNull T> dispatch(@NotNull Property<T, A> discriminant,
                                                @NotNull Function<A, Option<Record<? extends T>>> dispatch) {
        Objects.requireNonNull(discriminant);
        Objects.requireNonNull(dispatch);
        return new Dispatch<>(discriminant, dispatch);
    }

    static <T> @NotNull Template<@NotNull T> functional(@NotNull Class<T> type,
                                                        @NotNull Template<?> returns) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(returns);
        return new Functional<>(type, returns);
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

    static <T, A> @NotNull Record<T> record(@NotNull Property<T, A> a,
                                            @NotNull MonoConstructor<T, A> constructor) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(constructor);
        return new MonoRecord<>(a, constructor);
    }

    static <T, A, B> @NotNull Record<T> record(@NotNull Property<T, A> a,
                                               @NotNull Property<T, B> b,
                                               @NotNull DiConstructor<T, A, B> constructor) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(constructor);
        return new DiRecord<>(a, b, constructor);
    }

    static <T, A, B, C> @NotNull Record<T> record(@NotNull Property<T, A> a,
                                                  @NotNull Property<T, B> b,
                                                  @NotNull Property<T, C> c,
                                                  @NotNull TriConstructor<T, A, B, C> constructor) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(c);
        Objects.requireNonNull(constructor);
        return new TriRecord<>(a, b, c, constructor);
    }

    static <T, A, B, C, D> @NotNull Record<T> record(@NotNull Property<T, A> a,
                                                     @NotNull Property<T, B> b,
                                                     @NotNull Property<T, C> c,
                                                     @NotNull Property<T, D> d,
                                                     @NotNull TetraConstructor<T, A, B, C, D> constructor) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(c);
        Objects.requireNonNull(d);
        Objects.requireNonNull(constructor);
        return new TetraRecord<>(a, b, c, d, constructor);
    }

    static <T, A, B, C, D, E> @NotNull Record<T> record(@NotNull Property<T, A> a,
                                                        @NotNull Property<T, B> b,
                                                        @NotNull Property<T, C> c,
                                                        @NotNull Property<T, D> d,
                                                        @NotNull Property<T, E> e,
                                                        @NotNull PentaConstructor<T, A, B, C, D, E> constructor) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(c);
        Objects.requireNonNull(d);
        Objects.requireNonNull(e);
        Objects.requireNonNull(constructor);
        return new PentaRecord<>(a, b, c, d, e, constructor);
    }

    default <V> Property.Required<V, T> property(@NotNull String name,
                                                 @NotNull Accessor<V, T> accessor) {
        Objects.requireNonNull(name);
        return new Property.Required<>(name, this, accessor);
    }

    default <V> @NotNull Template<V> map(@NotNull Function<T, V> forward,
                                         @NotNull Function<V, T> backward) {
        Objects.requireNonNull(forward);
        Objects.requireNonNull(backward);
        return new Template<>() {

            @Override
            public @NotNull Option<V> lower(@NotNull Value value) {
                return Template.this.lower(value)
                        .map(forward);
            }

            @Override
            public @NotNull Option<@NotNull Value> raise(@Nullable V value) {
                return Template.this.raise(backward.apply(value));
            }

            @Override
            public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
                return Template.this.describe(points);
            }
        };
    }

    default <V> @NotNull Template<V> flatMap(@NotNull Function<T, @NotNull Option<V>> forward,
                                             @NotNull Function<@Nullable V, @NotNull Option<T>> backward) {
        Objects.requireNonNull(forward);
        Objects.requireNonNull(backward);
        return new Template<>() {

            @Override
            public @NotNull Option<V> lower(@NotNull Value value) {
                return Template.this.lower(value)
                        .flatMap(forward);
            }

            @Override
            public @NotNull Option<@NotNull Value> raise(@Nullable V value) {
                return backward.apply(value)
                        .flatMap(Template.this::raise);
            }

            @Override
            public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
                return Template.this.describe(points);
            }
        };
    }

    default @NotNull Template<T> filter(@NotNull Predicate<T> forward,
                                        @NotNull Predicate<@Nullable T> backward) {
        Objects.requireNonNull(forward);
        Objects.requireNonNull(backward);
        return new Template<>() {

            @Override
            public @NotNull Option<T> lower(@NotNull Value value) {
                return Template.this.lower(value)
                        .filter(forward);
            }

            @Override
            public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
                if (backward.test(value))
                    return Template.this.raise(value);
                return Option.none();
            }

            @Override
            public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
                return Template.this.describe(points);
            }
        };
    }

    @NotNull Option<T> lower(@NotNull Value value);

    @NotNull Option<@NotNull Value> raise(@Nullable T value);

    @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points);
}
