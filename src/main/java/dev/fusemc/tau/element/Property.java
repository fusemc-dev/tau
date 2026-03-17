package dev.fusemc.tau.element;

import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.dictionary.record.PropertyDescription;
import com.manchickas.optionated.Option;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

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

    @ApiStatus.Internal
    public @NotNull String name() {
        return this.name;
    }

    @ApiStatus.Internal
    protected abstract @NotNull Option<A> missing();

    @ApiStatus.Internal
    public abstract @NotNull PropertyDescription description(@NotNull Scope<Mu<?>> points);

    /// Represents a required property.
    ///
    /// On [#parse(org.graalvm.polyglot.Value)], a **required** property requires its
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
        /// Constructs a `Property` that, on [#parse(org.graalvm.polyglot.Value)], if
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
        public @NotNull PropertyDescription description(@NotNull Scope<Mu<?>> points) {
            return Description.property(this.name, this.template.description(points));
        }
    }

    /// Represents an optional property.
    ///
    /// On [#parse(org.graalvm.polyglot.Value)], an **optional** property uses
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
        public @NotNull PropertyDescription description(@NotNull Scope<Mu<?>> points) {
            return Description.optional(Description.property(this.name, this.template.description(points)));
        }
    }
}
