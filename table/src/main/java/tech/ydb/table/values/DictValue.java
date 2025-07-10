package tech.ydb.table.values;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class DictValue implements Value<DictType> {
    private static final long serialVersionUID = 6205349432501922070L;

    private final DictType type;
    private final HashMap<Value<?>, Value<?>> items;

    DictValue(DictType type, HashMap<Value<?>, Value<?>> items) {
        this.type = type;
        this.items = items;
    }

    public static DictValue of(Value<?> key, Value<?> value) {
        HashMap<Value<?>, Value<?>> map = new HashMap<>();
        map.put(key, value);
        return new DictValue(DictType.of(key.getType(), value.getType()), map);
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean contains(Value<?> key) {
        return items.containsKey(key);
    }

    @Nullable
    public Value<?> get(Value<?> key) {
        return items.get(key);
    }

    public Set<Value<?>> keySet() {
        return items.keySet();
    }

    public Collection<Value<?>> values() {
        return items.values();
    }

    public Set<Map.Entry<Value<?>, Value<?>>> entrySet() {
        return items.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DictValue that = (DictValue) o;
        return items.equals(that.items);
    }

    @Override
    public int hashCode() {
        return 31 * Type.Kind.DICT.hashCode() + items.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dict[");
        for (Map.Entry<Value<?>, Value<?>> e : items.entrySet()) {
            sb.append(e.getKey()).append(": ");
            sb.append(e.getValue()).append(", ");
        }
        if (!items.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public DictType getType() {
        return type;
    }

    @Override
    public ValueProtos.Value toPb() {
        if (isEmpty()) {
            return ProtoValue.dict();
        }

        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (Map.Entry<Value<?>, Value<?>> e : items.entrySet()) {
            ValueProtos.Value key = e.getKey().toPb();
            ValueProtos.Value value = e.getValue().toPb();

            builder.addPairsBuilder()
                .setKey(key)
                .setPayload(value);
        }
        return builder.build();
    }
}
