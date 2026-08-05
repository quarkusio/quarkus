package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.rest.data.panache.RestDataResourceMethodListener;
import io.quarkus.security.ForbiddenException;

@ApplicationScoped
public class ForbiddenItemListener implements RestDataResourceMethodListener<AbstractItem> {

    @Override
    public void onBeforeAdd(AbstractItem item) {
        throw new ForbiddenException("Adding items is forbidden");
    }
}
