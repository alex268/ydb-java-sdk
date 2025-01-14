package tech.ydb.topic.write;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import tech.ydb.topic.description.MetadataItem;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class Metadata {
    private final Instant createdAt;
    private final List<MetadataItem> items;

    private Metadata(Instant createdAt, List<MetadataItem> items) {
        this.createdAt = createdAt;
        this.items = items;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public List<MetadataItem> getItems() {
        return this.items;
    }

    public static Metadata empty() {
        return new Metadata(Instant.now(), Collections.emptyList());
    }

    public static Metadata of(@Nonnull Instant createdAt) {
        return new Metadata(createdAt, Collections.emptyList());
    }

    public Metadata withMetadataItem(@Nonnull String key, byte[] value) {
        MetadataItem item = new MetadataItem(key, value);
        if (items.isEmpty()) {
            return new Metadata(createdAt, Collections.singletonList(item));
        }
        List<MetadataItem> list = new ArrayList<>(items.size() + 1);
        list.addAll(items);
        list.add(item);
        return new Metadata(createdAt, Collections.unmodifiableList(list));
    }

    public Metadata withMetadataItems(List<MetadataItem> list) {
        if (list == null || list.isEmpty()) {
            return this;
        }
        if (items.isEmpty()) {
            return new Metadata(createdAt, Collections.unmodifiableList(list));
        }

        List<MetadataItem> newList = new ArrayList<>(items.size() + list.size());
        newList.addAll(list);
        newList.addAll(list);
        return new Metadata(createdAt, Collections.unmodifiableList(newList));
    }
}
