package dev.fusemc.tau.template.dictionary;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Scope;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.description.Description;
import dev.fusemc.tau.description.Domain;
import dev.fusemc.tau.proxy.MapLike;
import dev.fusemc.tau.template.Mu;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record HashLike<K, V>(@NotNull Template<K> key,
                             @NotNull Template<V> value) implements Template<@NotNull Map<K, V>> {

    public HashLike {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
    }

    @Override
    @SuppressWarnings("PatternVariableHidesField")
    public @NotNull Option<@NotNull Map<K, V>> lower(@NotNull Value value) {
        if (value.hasHashEntries()) {
            var buffer = new LinkedHashMap<K, V>((int) value.getHashSize());
            var iterator = value.getHashKeysIterator();
            while (iterator.hasIteratorNextElement()) {
                var entry = iterator.getIteratorNextElement();
                var option = this.key.lower(entry);
                if (option instanceof Option.Some<K>(var key)) {
                    var member = value.getHashValue(entry);
                    var mapping = this.value.lower(member);
                    if (mapping instanceof Option.Some<V>(var mapped)) {
                        buffer.put(key, mapped);
                        continue;
                    }
                    return Option.none();
                }
                return Option.none();
            }
            return Option.some(Map.copyOf(buffer));
        }
        if (value.hasMembers()) {
            var keys = value.getMemberKeys();
            var buffer = new LinkedHashMap<K, V>(keys.size());
            for (var entry : keys) {
                var option = this.key.lower(Value.asValue(entry));
                if (option instanceof Option.Some<K>(var key)) {
                    var member = value.getMember(entry);
                    var mapping = this.value.lower(Value.asValue(member));
                    if (mapping instanceof Option.Some<V>(var mapped)) {
                        buffer.put(key, mapped);
                        continue;
                    }
                    return Option.none();
                }
                return Option.none();
            }
            return Option.some(Map.copyOf(buffer));
        }
        if (value.isHostObject()) {
            var host = value.asHostObject();
            if (host instanceof Map<?, ?> map) {
                var buffer = new LinkedHashMap<K, V>(map.size());
                for (var entry : map.entrySet()) {
                    var option = this.key.lower(Value.asValue(entry.getKey()));
                    if (option instanceof Option.Some<K>(var key)) {
                        var mapping = this.value.lower(Value.asValue(entry.getValue()));
                        if (mapping instanceof Option.Some<V>(var mapped)) {
                            buffer.put(key, mapped);
                            continue;
                        }
                        return Option.none();
                    }
                    return Option.none();
                }
                return Option.some(Map.copyOf(buffer));
            }
            return Option.none();
        }
        if (value.isProxyObject()) {
            var proxy = value.asProxyObject();
            if (proxy instanceof ProxyObject object) {
                var entries = Tau.lower(
                        Template.array(Template.STRING, String[]::new),
                        Value.asValue(object.getMemberKeys())
                );
                var buffer = new LinkedHashMap<K, V>(entries.length);
                for (var entry : entries) {
                    var option = this.key.lower(Value.asValue(entry));
                    if (option instanceof Option.Some<K>(var key)) {
                        var mapping = this.value.lower(Value.asValue(object.getMember(entry)));
                        if (mapping instanceof Option.Some<V>(var mapped)) {
                            buffer.put(key, mapped);
                            continue;
                        }
                        return Option.none();
                    }
                    return Option.none();
                }
                return Option.some(Map.copyOf(buffer));
            }
            if (proxy instanceof ProxyHashMap map) {
                var buffer = new LinkedHashMap<K, V>((int) map.getHashSize());
                var entries = Tau.lower(
                        Template.iterator(Template.<Value[], Value, Value>tuple(
                                Template.element(Template.ANY, entry -> entry[0]),
                                Template.element(Template.ANY, entry -> entry[1]),
                                (a, b) -> new Value[]{a, b}
                        )),
                        Value.asValue(map.getHashEntriesIterator())
                );
                while (entries.hasNext()) {
                    var entry = entries.next();
                    var option = this.key.lower(entry[0]);
                    if (option instanceof Option.Some<K>(var key)) {
                        var mapping = this.value.lower(entry[1]);
                        if (mapping instanceof Option.Some<V>(var mapped)) {
                            buffer.put(key, mapped);
                            continue;
                        }
                        return Option.none();
                    }
                    return Option.none();
                }
                return Option.some(Map.copyOf(buffer));
            }
            return Option.none();
        }
        return Option.none();
    }

    @Override
    @SuppressWarnings("PatternVariableHidesField")
    public @NotNull Option<@NotNull Value> raise(@Nullable Map<K, V> value) {
        if (value != null) {
            var builder = MapLike.builder(value.size());
            for (var entry : value.entrySet()) {
                var option = this.key.raise(entry.getKey());
                if (option instanceof Option.Some<Value>(var key)) {
                    var mapping = this.value.raise(entry.getValue());
                    if (mapping instanceof Option.Some<Value>(var mapped)) {
                        builder.append(key, mapped);
                        continue;
                    }
                    return Option.none();
                }
                return Option.none();
            }
            return Option.some(Value.asValue(builder.build()));
        }
        return Option.none();
    }

    @Override
    public @NotNull Description describe(@NotNull Scope<@NotNull Mu<?>> points) {
        return Description.attach(
                Description.concat(
                        Description.delimiter('{'),
                        Description.concat(
                                Description.delimiter('['),
                                this.key.describe(points),
                                Description.delimiter(']')
                        ),
                        Description.delimiter(": "),
                        this.value.describe(points),
                        Description.delimiter('}')
                ),
                Domain.TEMPLATE
        );
    }
}
