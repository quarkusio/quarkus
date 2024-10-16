package io.quarkus.it.hibernate.search.standalone.elasticsearch.management;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyDatastore {
    private Map<UUID, ManagementTestEntity> content = new ConcurrentHashMap<>();

    public void clear() {
        content.clear();
    }

    public void put(ManagementTestEntity entity) {
        content.put(entity.getId(), entity);
    }

    public Map<UUID, ManagementTestEntity> getContent() {
        return Collections.unmodifiableMap(content);
    }
}
