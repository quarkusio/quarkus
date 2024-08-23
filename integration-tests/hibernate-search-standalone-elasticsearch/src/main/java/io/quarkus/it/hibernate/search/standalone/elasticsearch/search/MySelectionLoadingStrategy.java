/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package io.quarkus.it.hibernate.search.standalone.elasticsearch.search;

import java.util.List;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;

import io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub.DatastoreConnectionStub;

// See https://docs.jboss.org/hibernate/search/7.1/reference/en-US/html_single/#mapping-entitydefinition-loading-selection
public class MySelectionLoadingStrategy<E>
        implements SelectionLoadingStrategy<E> {
    private final Class<E> rootEntityType;

    public MySelectionLoadingStrategy(Class<E> rootEntityType) {
        this.rootEntityType = rootEntityType;
    }

    @Override
    public SelectionEntityLoader<E> createEntityLoader(
            LoadingTypeGroup<E> includedTypes,
            SelectionLoadingOptions options) {
        DatastoreConnectionStub connection = options.context(DatastoreConnectionStub.class);
        return new SelectionEntityLoader<E>() {
            @Override
            public List<E> load(List<?> identifiers, Deadline deadline) {
                return connection.loadEntitiesByIdInSameOrder(
                        rootEntityType, identifiers);
            }
        };
    }
}
