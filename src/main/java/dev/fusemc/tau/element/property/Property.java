package dev.fusemc.tau.element.property;

import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.element.Accessor;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

public sealed abstract class Property<T, A> {

    protected final @NotNull String name;
    protected final @NotNull Template<A> template;
    protected final @NotNull Accessor<T, A> accessor;

    protected Property(@NotNull String name,
                       @NotNull Template<A> template,
                       @NotNull Accessor<T, A> accessor) {
        this.name     = Objects.requireNonNull(name);
        this.template = Objects.requireNonNull(template);
        this.accessor = Objects.requireNonNull(accessor);
    }

    public @NotNull Option<A> parse(@NotNull Value value) {
        Objects.requireNonNull(value);
        if (value.hasMembers()) {
            var member = value.getMember(this.name);
            if (member != null)
                return this.template.parse(member);
            return this.missing();
        }
        return Option.none();
    }

    public @NotNull Option<Value> serialize(@NotNull T instance) {
        Objects.requireNonNull(instance);
        var property = this.accessor.access(instance);
        return this.template.serialize(property);
    }

    public @NotNull String name() {
        return this.name;
    }

    protected abstract @NotNull Option<A> missing();

    public abstract @NotNull dev.fusemc.tau.description.dictionary.record.Property description(@NotNull Scope<Mu<?>> points);

    public static final class Required<T, A> extends Property<T, A> {

        public Required(@NotNull String name,
                        @NotNull Template<A> template,
                        @NotNull Accessor<T, A> accessor) {
            super(name, template, accessor);
        }

        @Override
        protected @NotNull Option<A> missing() {
            return Option.none();
        }

        @Override
        public dev.fusemc.tau.description.dictionary.record.@NotNull Property description(@NotNull Scope<Mu<?>> points) {
            return Description.property(this.name, this.template.description(points));
        }
    }

    public static final class Optional<T, A> extends Property<T, A> {

        private final @NotNull Supplier<A> supplier;

        public Optional(@NotNull Property.Required<T, A> property,
                        @NotNull Supplier<A> supplier) {
            super(property.name, property.template, property.accessor);
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected @NotNull Option<A> missing() {
            return Option.some(this.supplier.get());
        }

        @Override
        public dev.fusemc.tau.description.dictionary.record.@NotNull Property description(@NotNull Scope<Mu<?>> points) {
            return Description.optional(Description.property(this.name, this.template.description(points)));
        }
    }
}
