package io.quarkus.it.hibernate.search.standalone.elasticsearch.search;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;

import io.quarkus.arc.Unremovable;
import io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub.DatastoreStub;

@ApplicationScoped
@Unremovable
public class MyLoadingBinder implements EntityLoadingBinder {
    @Inject
    private DatastoreStub datastore;

    public MyLoadingBinder() {
    }

    @Override
    public void bind(EntityLoadingBindingContext context) {
        bind(context, context.entityType().rawType());
    }

    private <T> void bind(EntityLoadingBindingContext context, Class<T> type) {
        context.massLoadingStrategy(type,
                new MyMassLoadingStrategy<>(datastore, type));
        context.selectionLoadingStrategy(type,
                new MySelectionLoadingStrategy<>(type));
    }

}
