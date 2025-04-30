package io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DatastoreStub {

    final Map<Class<?>, Map<Long, Object>> entities = new LinkedHashMap<>();

    public DatastoreConnectionStub connect() {
        return new DatastoreConnectionStub(this);
    }
}
