/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package io.quarkus.it.hibernate.search.standalone.elasticsearch.management;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class MyLoadingBinder implements EntityLoadingBinder {

    @Inject
    MyDatastore datastore;

    @Override
    public void bind(EntityLoadingBindingContext context) {
        context.massLoadingStrategy(ManagementTestEntity.class, MassLoadingStrategy.fromMap(datastore.getContent()));
    }
}
