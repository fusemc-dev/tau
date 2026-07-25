package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.function.FunctionLike;
import dev.fusemc.tau.function.convention.Convention;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Objects;

public final class Functional<T> implements Template<T> {

    private final @NotNull Class<T> type;
    private final @NotNull Method target;
    private final @NotNull Template<?> template;

    @SuppressWarnings("PatternVariableHidesField")
    public Functional(@NotNull Class<T> type,
                      @NotNull Template<?> template) {
        var option = Functional.findTarget(type);
        if (option instanceof Option.Some<Method>(var target)) {
            this.type     = Objects.requireNonNull(type);
            this.target   = Objects.requireNonNull(target);
            this.template = Objects.requireNonNull(template);
            return;
        }
        throw new AssertionError();
    }

    /// Attempt to find an interface method suitable for implementation.
    ///
    /// ---
    ///
    /// If the provided `type` is an **interface** with a single, unambiguous **abstract method**, it is
    /// considered the target of the `Functional`. Otherwise, the type is considered incompatible
    /// and `Option.none()` is returned.
    ///
    /// @since 0.1.0
    @ApiStatus.Internal
    public static @NotNull Option<Method> findTarget(@NotNull Class<?> type) {
        Objects.requireNonNull(type);
        if (type.isInterface()) {
            var haystack = type.getMethods();
            var needle = (Method) null;
            for (var method : haystack) {
                var flags = method.accessFlags();
                if (flags.contains(AccessFlag.ABSTRACT)) {
                    if (needle == null) {
                        needle = method;
                        continue;
                    }
                    return Option.none();
                }
            }
            if (needle != null)
                return Option.some(needle);
            return Option.none();
        }
        return Option.none();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Option<T> lower(@NotNull Value value) {
        if (value.canExecute()) {
            var handler = new FunctionLike<>(this.target, this.template, Convention.POLYGLOT, value);
            return Option.some((T) Proxy.newProxyInstance(
                    Tau.class.getClassLoader(),
                    new Class<?>[] { this.type },
                    handler
            ));
        }
        if (value.isProxyObject()) {
            var proxy = value.asProxyObject();
            if (this.type.isInstance(proxy))
                return Option.some(this.type.cast(proxy));
            if (proxy instanceof ProxyExecutable executable) {
                var handler = new FunctionLike<>(this.target, this.template, Convention.PROXY, executable);
                return Option.some((T) Proxy.newProxyInstance(
                        Tau.class.getClassLoader(),
                        new Class<?>[] { this.type },
                        handler
                ));
            }
            return Option.none();
        }
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (this.type.isInstance(host))
                return Option.some(this.type.cast(host));
            return Option.none();
        }
        return Option.none();
    }

    @Override
    public @NotNull Option<@NotNull Value> raise(@Nullable T value) {
        if (value != null) {
            var type = value.getClass();
            if (Proxy.isProxyClass(type)) {
                var handler = Proxy.getInvocationHandler(value);
                if (handler instanceof FunctionLike<?> fn)
                    return Option.some(Value.asValue(fn.unwrap()));
                return Option.none();
            }
            return Option.some(Value.asValue(value));
        }
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.concat(
                Description.concat(
                        Description.delimiter('('),
                        Description.join(Description.delimiter(", "), Arrays.stream(this.target.getParameters())
                                .map(parameter -> {
                                    var description = Tau.describe(parameter.getParameterizedType());
                                    if (parameter.isVarArgs())
                                        return Description.concat(Description.ELLIPSIS, description);
                                    return description;
                                })
                                .toArray(Description[]::new)),
                        Description.delimiter(')')
                ),
                Description.delimiter(" => "),
                this.template.describe(points)
        );
    }

    @Override
    public @NotNull String toString() {
        return Template.toString(this);
    }
}
