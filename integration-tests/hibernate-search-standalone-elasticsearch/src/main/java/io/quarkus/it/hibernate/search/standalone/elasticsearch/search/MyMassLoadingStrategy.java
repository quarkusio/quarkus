/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package io.quarkus.it.hibernate.search.standalone.elasticsearch.search;

import java.util.Collection;
import java.util.List;
import java.util.OptionalLong;

import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;

import io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub.DatastoreConnectionStub;
import io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub.DatastoreCursorStub;
import io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub.DatastoreStub;

// See https://docs.jboss.org/hibernate/search/7.1/reference/en-US/html_single/#mapping-entitydefinition-loading-mass
public class MyMassLoadingStrategy<E>
        implements MassLoadingStrategy<E, Long> {

    private final DatastoreStub datastore;
    private final Class<E> rootEntityType;

    public MyMassLoadingStrategy(DatastoreStub datastore, Class<E> rootEntityType) {
        this.datastore = datastore;
        this.rootEntityType = rootEntityType;
    }

    @Override
    public MassIdentifierLoader createIdentifierLoader(
            LoadingTypeGroup<E> includedTypes,
            MassIdentifierSink<Long> sink, MassLoadingOptions options) {
        int batchSize = options.batchSize();
        Collection<Class<? extends E>> typeFilter = includedTypes.includedTypesMap().values();
        return new MassIdentifierLoader() {
            private final DatastoreConnectionStub connection = datastore.connect();
            private final DatastoreCursorStub<Long> identifierCursor = connection.scrollIdentifiers(typeFilter);

            @Override
            public void close() {
                connection.close();
            }

            @Override
            public OptionalLong totalCount() {
                return OptionalLong.of(connection.countEntities(typeFilter));
            }

            @Override
            public void loadNext() throws InterruptedException {
                List<Long> batch = identifierCursor.next(batchSize);
                if (batch != null) {
                    sink.accept(batch);
                } else {
                    sink.complete();
                }
            }
        };
    }

    @Override
    public MassEntityLoader<Long> createEntityLoader(
            LoadingTypeGroup<E> includedTypes,
            MassEntitySink<E> sink, MassLoadingOptions options) {
        return new MassEntityLoader<>() {
            private final DatastoreConnectionStub connection = datastore.connect();

            @Override
            public void close() {
                connection.close();
            }

            @Override
            public void load(List<Long> identifiers)
                    throws InterruptedException {
                sink.accept(connection.loadEntitiesById(rootEntityType, identifiers));
            }
        };
    }
}
