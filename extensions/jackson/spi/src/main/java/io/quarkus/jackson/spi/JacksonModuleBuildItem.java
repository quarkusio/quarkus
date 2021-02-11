package io.quarkus.jackson.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem used to create a Jackson SimpleModule for the purpose of registering
 * serializers and deserializers
 *
 * Serializers and deserializers MUST contain a public no-args constructor
 */
public final class JacksonModuleBuildItem extends MultiBuildItem {

    private final String name;
    private final Collection<Item> items;

    public JacksonModuleBuildItem(String name, Collection<Item> items) {
        this.name = name;
        this.items = items;
    }

    public String getName() {
        return name;
    }

    public Collection<Item> getItems() {
        return items;
    }

    public static class Builder {
        private final String name;
        private final Map<String, String> targetClassToSerializer = new HashMap<>();
        private final Map<String, String> targetClassToDeserializer = new HashMap<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder addSerializer(String serializerClassName, String targetClassName) {
            this.targetClassToSerializer.put(targetClassName, serializerClassName);
            return this;
        }

        public Builder addDeserializer(String deserializerClassName, String targetClassName) {
            this.targetClassToDeserializer.put(targetClassName, deserializerClassName);
            return this;
        }

        public Builder add(String serializerClassName, String deserializerClassName, String targetClassName) {
            this.targetClassToSerializer.put(targetClassName, serializerClassName);
            this.targetClassToDeserializer.put(targetClassName, deserializerClassName);
            return this;
        }

        public JacksonModuleBuildItem build() {
            Set<String> allTargetClassNames = new HashSet<>(targetClassToSerializer.keySet());
            allTargetClassNames.addAll(targetClassToDeserializer.keySet());
            List<Item> items = new ArrayList<>(targetClassToSerializer.size());
            for (String targetClassName : allTargetClassNames) {
                items.add(new Item(targetClassName, targetClassToSerializer.get(targetClassName),
                        targetClassToDeserializer.get(targetClassName)));
            }
            return new JacksonModuleBuildItem(this.name, items);
        }
    }

    // This is needed because Jackson can't register a deserializer without the target type
    public static class Item {
        private final String targetClassName;
        private final String serializerClassName;
        private final String deserializerClassName;

        public Item(String targetClassName, String serializerClassName, String deserializerClassName) {
            this.serializerClassName = serializerClassName;
            this.deserializerClassName = deserializerClassName;
            if (targetClassName == null || targetClassName.isEmpty()) {
                throw new IllegalArgumentException("targetClassName cannot be null or empty");
            }
            this.targetClassName = targetClassName;
        }

        public String getSerializerClassName() {
            return serializerClassName;
        }

        public String getDeserializerClassName() {
            return deserializerClassName;
        }

        public String getTargetClassName() {
            return targetClassName;
        }
    }
}
