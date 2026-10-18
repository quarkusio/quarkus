/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatastoreConnectionStub implements AutoCloseable {

    private final DatastoreStub datastore;

    public DatastoreConnectionStub(DatastoreStub datastore) {
        this.datastore = datastore;
    }

    public void put(Long id, Object entity) {
        datastore.entities.computeIfAbsent(entity.getClass(), ignored -> new LinkedHashMap<>())
                .put(id, entity);
    }

    @Override
    public void close() {
        // Nothing to do, this class is more or less a stub.
    }

    public DatastoreCursorStub<Long> scrollIdentifiers(Collection<? extends Class<?>> typeFilter) {
        if (typeFilter.size() != 1) {
            throw new IllegalArgumentException("This implementation only supports targeting one type at a time");
        }
        return new DatastoreCursorStub<>(datastore.entities.get(typeFilter.iterator().next()).keySet().iterator());
    }

    public long countEntities(Collection<? extends Class<?>> typeFilter) {
        if (typeFilter.size() != 1) {
            throw new IllegalArgumentException("This implementation only supports targeting one type at a time");
        }
        return datastore.entities.get(typeFilter.iterator().next()).keySet().size();
    }

    public <T> List<T> loadEntitiesById(Class<T> entityType, List<Long> identifiers) {
        return loadEntitiesByIdInSameOrder(entityType, identifiers);
    }

    public <T> List<T> loadEntitiesByIdInSameOrder(Class<T> entityType, List<?> identifiers) {
        Map<Long, ?> entities = datastore.entities.get(entityType);
        return identifiers.stream()
                .map(id -> entityType.cast(entities.get((Long) id)))
                .collect(Collectors.toList());
    }
}
