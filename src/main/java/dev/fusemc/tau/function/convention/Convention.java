package dev.fusemc.tau.function.convention;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface Convention<T> {

    @NotNull Convention<Value> POLYGLOT = (UniformConvention<Value>) (callee, args) -> {
        Objects.requireNonNull(callee);
        if (args != null)
            return callee.execute(args);
        return callee.execute();
    };

    @NotNull Convention<ProxyExecutable> PROXY = (UniformConvention<ProxyExecutable>) (callee, args) -> {
        Objects.requireNonNull(callee);
        if (args != null) {
            var buffer = new Value[args.length];
            for (var i = 0; i < args.length; i++)
                buffer[i] = Value.asValue(args[i]);
            return Value.asValue(callee.execute(buffer));
        }
        return Value.asValue(callee.execute());
    };

    @NotNull Value call(@NotNull T callee, Object @Nullable[] args);

    default @NotNull Value callVariadic(@NotNull T callee, Object @Nullable[] args) {
        return this.call(callee, args);
    }
}
