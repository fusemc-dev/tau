package dev.fusemc.tau.element;

import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/// Represents a single property of a record.
///
/// A `Property` associates a field `name` of a dictionary-like
/// with a property of type `A` on type `T`. A `Property` may either
/// be [Required] or [Optional].
///
/// @see Element
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

    public @NotNull Option<A> lower(@NotNull Value value) {
        Objects.requireNonNull(value);
        if (value.hasMembers() && !value.hasArrayElements()) {
            var member = value.getMember(this.name);
            if (member != null)
                return this.template.lower(member);
            return this.missing();
        }
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (host instanceof Map<?, ?> map) {
                var member = map.get(this.name);
                if (member instanceof Value v)
                    return this.template.lower(v);
                return this.missing();
            }
            return Option.none();
        }
        return Option.none();
    }

    public @NotNull Option<Value> raise(@NotNull T instance) {
        Objects.requireNonNull(instance);
        var property = this.access(instance);
        return this.template.raise(property);
    }

    public A access(@NotNull T instance) {
        Objects.requireNonNull(instance);
        return this.accessor.access(instance);
    }

    @ApiStatus.Internal
    public @NotNull String name() {
        return this.name;
    }

    @ApiStatus.Internal
    protected abstract @NotNull Option<A> missing();

    @ApiStatus.Internal
    public abstract @NotNull Description description(@NotNull Scope<Mu<?>> points);

    /// Represents a required property.
    ///
    /// On [#lower(org.graalvm.polyglot.Value)], a **required** property requires its
    /// associated field to be present in the dictionary-like for the parsing to succeed.
    ///
    /// @see Optional
    public static final class Required<T, A> extends Property<T, A> {

        public Required(@NotNull String name,
                        @NotNull Template<A> template,
                        @NotNull Accessor<T, A> accessor) {
            super(name, template, accessor);
        }

        @Override
        @ApiStatus.Internal
        protected @NotNull Option<A> missing() {
            return Option.none();
        }

        /// Constructs an [Optional] property.
        ///
        /// Constructs a `Property` that, on [#lower(org.graalvm.polyglot.Value)], if
        /// the associated field is missing, uses the provided [Supplier] to supply a fallback `A`.
        ///
        /// The following call to `optional` yields a `Property` that defaults to `42`:
        ///
        /// ```java
        /// Template.INTEGER.property("x", Vec3::x)
        ///     .optional(() -> 42);
        /// ```
        public @NotNull Optional<T, A> optional(@NotNull Supplier<A> supplier) {
            Objects.requireNonNull(supplier);
            return new Optional<>(this.name, this.template, this.accessor, supplier);
        }

        @Override
        @ApiStatus.Internal
        public @NotNull Description description(@NotNull Scope<Mu<?>> points) {
            return Description.concat(
                    Description.literal(this.name),
                    Description.delimiter(": "),
                    this.template.describe(points)
            );
        }
    }

    /// Represents an optional property.
    ///
    /// On [#lower(org.graalvm.polyglot.Value)], an **optional** property uses
    /// the provided [Supplier] to supply a fallback `A` if the associated field
    /// is missing in the dictionary-like.
    ///
    /// @see Required
    public static final class Optional<T, A> extends Property<T, A> {

        private final @NotNull Supplier<A> supplier;

        public Optional(@NotNull String name,
                        @NotNull Template<A> template,
                        @NotNull Accessor<T, A> accessor,
                        @NotNull Supplier<A> supplier) {
            super(name, template, accessor);
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        @ApiStatus.Internal
        protected @NotNull Option<A> missing() {
            return Option.some(this.supplier.get());
        }

        @Override
        @ApiStatus.Internal
        public @NotNull Description description(@NotNull Scope<Mu<?>> points) {
            return Description.concat(
                    Description.literal(this.name),
                    Description.delimiter("?: "),
                    this.template.describe(points)
            );
        }
    }
}
