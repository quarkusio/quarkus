package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import static io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_AFTER_DELETE_COUNTER;
import static io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_AFTER_SAVE_COUNTER;
import static io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_AFTER_UPDATE_COUNTER;
import static io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_BEFORE_DELETE_COUNTER;
import static io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_BEFORE_SAVE_COUNTER;
import static io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_BEFORE_UPDATE_COUNTER;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.rest.data.panache.RestDataResourceMethodListener;

@ApplicationScoped
public class ItemRestDataResourceMethodListener implements RestDataResourceMethodListener<Item> {

    @Override
    public void onBeforeAdd(Item item) {
        ON_BEFORE_SAVE_COUNTER.incrementAndGet();
    }

    @Override
    public void onAfterAdd(Item item) {
        ON_AFTER_SAVE_COUNTER.incrementAndGet();
    }

    @Override
    public void onBeforeUpdate(Item item) {
        ON_BEFORE_UPDATE_COUNTER.incrementAndGet();
    }

    @Override
    public void onAfterUpdate(Item item) {
        ON_AFTER_UPDATE_COUNTER.incrementAndGet();
    }

    @Override
    public void onBeforeDelete(Object id) {
        ON_BEFORE_DELETE_COUNTER.incrementAndGet();
    }

    @Override
    public void onAfterDelete(Object id) {
        ON_AFTER_DELETE_COUNTER.incrementAndGet();
    }
}
