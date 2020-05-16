package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import java.util.List;

import javax.ws.rs.core.Response;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(path = "custom-items", hal = true)
public interface CustomPathItemsResource extends PanacheEntityResource<Item, Long> {

    @MethodProperties(path = "api")
    List<Item> list();

    @MethodProperties(path = "api")
    Item get(Long id);

    @MethodProperties(path = "api")
    Response add(Item entity);

    @MethodProperties(path = "api")
    Response update(Long id, Item entity);

    @MethodProperties(path = "api")
    void delete(Long id);
}
