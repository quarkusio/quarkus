package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import static io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_AFTER_DELETE_COUNTER;
import static io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_AFTER_SAVE_COUNTER;
import static io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_AFTER_UPDATE_COUNTER;
import static io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_BEFORE_DELETE_COUNTER;
import static io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_BEFORE_SAVE_COUNTER;
import static io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.PanacheEntityResourceMethodListenerTest.ON_BEFORE_UPDATE_COUNTER;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.rest.data.panache.RestDataResourceMethodListener;

@ApplicationScoped
public class ItemRestDataResourceMethodListener implements RestDataResourceMethodListener<AbstractItem> {

    @Override
    public void onBeforeAdd(AbstractItem item) {
        ON_BEFORE_SAVE_COUNTER.incrementAndGet();
    }

    @Override
    public void onAfterAdd(AbstractItem item) {
        ON_AFTER_SAVE_COUNTER.incrementAndGet();
    }

    @Override
    public void onBeforeUpdate(AbstractItem item) {
        ON_BEFORE_UPDATE_COUNTER.incrementAndGet();
    }

    @Override
    public void onAfterUpdate(AbstractItem item) {
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
