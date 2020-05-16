package io.quarkus.rest.data.panache.deployment.methods;

import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.jboss.jandex.IndexView;
import org.jboss.resteasy.links.LinkResource;

import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.AnnotationCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;

public interface MethodImplementor {

    String APPLICATION_JSON = "application/json";

    String APPLICATION_HAL_JSON = "application/hal+json";

    void implement(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo);

    default void addTransactionalAnnotation(AnnotatedElement element) {
        element.addAnnotation(Transactional.class);
    }

    default void addGetAnnotation(AnnotatedElement element) {
        element.addAnnotation(GET.class);
    }

    default void addPostAnnotation(AnnotatedElement element) {
        element.addAnnotation(POST.class);
    }

    default void addPutAnnotation(AnnotatedElement element) {
        element.addAnnotation(PUT.class);
    }

    default void addDeleteAnnotation(AnnotatedElement element) {
        element.addAnnotation(DELETE.class);
    }

    default void addLinksAnnotation(AnnotatedElement element, String type, String rel) {
        AnnotationCreator linkResource = element.addAnnotation(LinkResource.class);
        linkResource.addValue("entityClassName", type);
        linkResource.addValue("rel", rel);
    }

    default void addPathAnnotation(AnnotatedElement element, String value) {
        element.addAnnotation(Path.class).addValue("value", value);
    }

    default void addPathParamAnnotation(AnnotatedElement element, String value) {
        element.addAnnotation(PathParam.class).addValue("value", value);
    }

    default void addProducesAnnotation(AnnotatedElement element, String... mediaTypes) {
        element.addAnnotation(Produces.class).addValue("value", mediaTypes);
    }

    default void addConsumesAnnotation(AnnotatedElement element, String... mediaTypes) {
        element.addAnnotation(Consumes.class).addValue("value", mediaTypes);
    }
}
