package dev.fusemc.tau.template;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Domain;
import dev.fusemc.tau.proxy.FunctionLike;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Objects;

public final class Functional<T> implements Template<T> {

    private final @NotNull Class<T> type;
    private final @NotNull Template<?> template;
    private final @NotNull Method target;

    @SuppressWarnings("PatternVariableHidesField")
    public Functional(@NotNull Class<T> type,
                      @NotNull Template<?> template) {
        var option = Functional.findTarget(type);
        if (option instanceof Option.Some<Method>(var target)) {
            this.type = Objects.requireNonNull(type);
            this.template = Objects.requireNonNull(template);
            this.target = target;
            return;
        }
        throw new AssertionError();
    }

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
            var handler = new FunctionLike(this.target, this.template, value);
            return Option.some((T) Proxy.newProxyInstance(
                    Tau.class.getClassLoader(),
                    new Class<?>[] { this.type },
                    handler
            ));
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
                if (handler instanceof FunctionLike impl)
                    return Option.some(impl.delegate());
                return Option.none();
            }
            return Option.some(Value.asValue(value));
        }
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(Description.concat(
                Description.concat(
                        Description.delimiter('('),
                        Description.join(Description.delimiter(", "), Arrays.stream(this.target.getParameters())
                                .map(parameter -> {
                                    var description = Tau.describe(parameter.getParameterizedType());
                                    if (parameter.isVarArgs())
                                        return Description.attach(Description.concat(
                                                Description.ELLIPSIS,
                                                description
                                        ), Domain.REFLECTION);
                                    return description;
                                })
                                .toArray(Description[]::new)),
                        Description.delimiter(')')
                ),
                Description.delimiter(" => "),
                this.template.describe(points)
        ), Domain.TEMPLATE);
    }

    @Override
    public @NotNull String toString() {
        return Template.toString(this);
    }
}
