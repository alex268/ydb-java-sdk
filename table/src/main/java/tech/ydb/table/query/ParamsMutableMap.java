package tech.ydb.table.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * Mutable implementation of {@link Params} interface.
 *
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
final class ParamsMutableMap implements Params {
    private static final long serialVersionUID = -2195403246008082524L;

    private final HashMap<String, Value<?>> params;

    ParamsMutableMap() {
        this.params = new HashMap<>();
    }

    ParamsMutableMap(int expectedSize) {
        this.params = Maps.newHashMapWithExpectedSize(expectedSize);
    }

    ParamsMutableMap(Map<String, Value<?>> params) {
        this.params = new HashMap<>(params);
    }

    @Override
    public boolean isEmpty() {
        return params.isEmpty();
    }

    @Override
    public <T extends Type> Params put(String name, Value<T> value) {
        Value<?> prev = params.putIfAbsent(name, value);
        Preconditions.checkArgument(prev == null, "duplicate parameter: %s", name);
        return this;
    }

    @Override
    public Map<String, ValueProtos.TypedValue> toPb() {
        Map<String, ValueProtos.TypedValue> result = Maps.newHashMapWithExpectedSize(params.size());
        for (Map.Entry<String, Value<?>> entry : params.entrySet()) {
            Value<?> value = entry.getValue();
            String name = entry.getKey();

            ValueProtos.TypedValue valuePb = ProtoValue.toTypedValue(value);
            result.put(name, valuePb);
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public Map<String, Value<?>> values() {
        return Collections.unmodifiableMap(params);
    }
}
