package dev.fusemc.tau.function;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.TypeException;
import dev.fusemc.tau.function.convention.Convention;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

/// Represents a function-like.
///
/// ---
/// A `FunctionLike` represents [InvocationHandler] that acts as an implementation of a **functional interface** in
/// terms of a delegate callee. A calling [Convention] is provided at construction that controls
/// how the arguments are processed.
///
/// @since 0.1.0
public final class FunctionLike<T> implements InvocationHandler {

    private static final Method TO_STRING;
    private static final Method HASH_CODE;
    private static final Method EQUALS;

    private final @NotNull Method target;
    private final @NotNull Template<?> template;
    private final @NotNull Convention<T> convention;
    private final @NotNull T callee;

    public FunctionLike(@NotNull Method target,
                        @NotNull Template<?> template,
                        @NotNull Convention<T> convention,
                        @NotNull T callee) {
        this.target     = Objects.requireNonNull(target);
        this.template   = Objects.requireNonNull(template);
        this.convention = Objects.requireNonNull(convention);
        this.callee     = Objects.requireNonNull(callee);
    }

    @Override
    public Object invoke(@NotNull Object proxy, @NotNull Method method, Object[] args) throws Throwable {
        Objects.requireNonNull(proxy);
        Objects.requireNonNull(method);
        if (method.equals(FunctionLike.TO_STRING))
            return this.callee.toString();
        if (method.equals(FunctionLike.HASH_CODE))
            return this.callee.hashCode();
        if (method.equals(FunctionLike.EQUALS)) {
            var other = args[0];
            if (Proxy.isProxyClass(other.getClass()))
                return this.equals(Proxy.getInvocationHandler(other));
            return false;
        }
        if (method.equals(this.target)) {
            var value  = Tau.lower(this.template, this.target.isVarArgs()
                    ? this.convention.callVariadic(this.callee, args)
                    : this.convention.call(this.callee, args));
            var option = FunctionLike.cast(this.target.getReturnType(), value);
            if (option instanceof Option.Some<?>(var wrapped))
                return wrapped;
            throw new TypeException(Tau.describe(value), Tau.describe(this.target.getGenericReturnType()));
        }
        if (method.isDefault()) {
            var lookup         = MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup());
            var implementation = lookup.unreflectSpecial(method, method.getDeclaringClass()).bindTo(proxy);
            if (args == null)
                return implementation.invokeWithArguments();
            return implementation.invokeWithArguments(args);
        }
        throw new AssertionError();
    }

    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    private static <T> @NotNull Option<T> cast(@NotNull Class<T> clazz, @Nullable Object value) {
        if (clazz == byte.class) {
            if (value instanceof Byte b)
                return Option.some((T) b);
            return Option.none();
        }
        if (clazz == short.class) {
            if (value instanceof Short s)
                return Option.some((T) s);
            return Option.none();
        }
        if (clazz == int.class) {
            if (value instanceof Integer i)
                return Option.some((T) i);
            return Option.none();
        }
        if (clazz == long.class) {
            if (value instanceof Long l)
                return Option.some((T) l);
            return Option.none();
        }
        if (clazz == float.class) {
            if (value instanceof Float f)
                return Option.some((T) f);
            return Option.none();
        }
        if (clazz == double.class) {
            if (value instanceof Double d)
                return Option.some((T) d);
            return Option.none();
        }
        if (clazz == boolean.class) {
            if (value instanceof Boolean b)
                return Option.some((T) b);
            return Option.none();
        }
        if (clazz == char.class) {
            if (value instanceof Character c)
                return Option.some((T) c);
            return Option.none();
        }
        if (clazz == void.class) {
            if (value == null)
                return Option.some(null);
            return Option.none();
        }
        if (clazz.isInstance(value))
            return Option.some(clazz.cast(value));
        return Option.none();
    }

    public @NotNull T unwrap() {
        return this.callee;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof FunctionLike<?> other)
            return this.target.equals(other.target)
                    && this.template.equals(other.template)
                    && this.convention.equals(other.convention)
                    && this.callee.equals(other.callee);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.target, this.template, this.convention, this.callee);
    }

    static {
        try {
            TO_STRING = Object.class.getDeclaredMethod("toString");
            HASH_CODE = Object.class.getDeclaredMethod("hashCode");
            EQUALS    = Object.class.getDeclaredMethod("equals", Object.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }
}
