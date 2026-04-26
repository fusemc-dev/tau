package dev.fusemc.tau.template.collection;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;

public record Iterable<T>(@NotNull Template<T> element) implements Template<Iterator<T>> {

    public Iterable {
        Objects.requireNonNull(element);
    }

    /// Attempts to `lower` the provided [Value] as an [Iterator].
    ///
    /// The returned `Iterator` will **lazily** lower the received elements using
    /// the associated `element` `Template`.
    ///
    /// @see #raise(Iterator)
    /// @see #describe(Scope)
    /// @since `0.1.0`
    @Override
    public @NotNull Option<Iterator<T>> lower(@NotNull Value value) {
        if (value.isIterator())
            return Option.some(new Iterator<>() {

                @Override
                public T next() {
                    return Tau.lower(Iterable.this.element, value.getIteratorNextElement());
                }

                @Override
                public boolean hasNext() {
                    return value.hasIteratorNextElement();
                }
            });
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (host instanceof Iterator<?> iterator)
                return Option.some(new Iterator<>() {

                    @Override
                    public T next() {
                        return Tau.lower(Iterable.this.element, Value.asValue(iterator.next()));
                    }

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }
                });
            return Option.none();
        }
        if (value.isProxyObject()) {
            var proxy = value.asProxyObject();
            if (proxy instanceof ProxyIterator iterator)
                return Option.some(new Iterator<>() {

                    @Override
                    public T next() {
                        return Tau.lower(Iterable.this.element, Value.asValue(iterator.getNext()));
                    }

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }
                });
            return Option.none();
        }
        return Option.none();
    }

    /// Attempts to `lower` the provided [Iterator] as a [ProxyIterator].
    ///
    /// The returned `Iterator` will **lazily** raise the received elements using
    /// the associated `element` `Template`.
    ///
    /// @see #lower(Value) 
    /// @see #describe(Scope)
    /// @since `0.1.0`
    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable Iterator<T> value) {
        if (value != null)
            return Option.some(Value.asValue(new ProxyIterator() {

                @Override
                public Object getNext() {
                    return Iterable.this.element.raise(value.next());
                }

                @Override
                public boolean hasNext() {
                    return value.hasNext();
                }
            }));
        return Option.none();
    }

    /// Describes the `Iterable`.
    ///
    /// The returned [Description] will be annotated as having come from [Domain#TEMPLATE].
    ///
    /// An `Iterable` is described as follows, where `δ` denotes the `Description`
    /// of the associated `element` [Template]:
    ///
    /// ```
    /// ...δ
    /// ```
    /// 
    /// @since `0.1.0`
    /// @see #lower(Value) 
    /// @see #raise(Iterator) 
    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Description.concat(
                Description.delimiter("..."),
                this.element.describe(points)
        ), Domain.TEMPLATE);
    }

    @Override
    public @NotNull String toString() {
        return Template.toString(this);
    }
}
