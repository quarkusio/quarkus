package io.quarkus.panache.rest.common.deployment.utils;

import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.jboss.resteasy.links.LinkResource;

import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.AnnotationCreator;

public final class ResourceAnnotator {

    public static final String APPLICATION_JSON = "application/json";

    public static final String APPLICATION_HAL_JSON = "application/hal+json";

    public static void addTransactional(AnnotatedElement element) {
        element.addAnnotation(Transactional.class);
    }

    public static void addGet(AnnotatedElement element) {
        element.addAnnotation(GET.class);
    }

    public static void addPost(AnnotatedElement element) {
        element.addAnnotation(POST.class);
    }

    public static void addPut(AnnotatedElement element) {
        element.addAnnotation(PUT.class);
    }

    public static void addDelete(AnnotatedElement element) {
        element.addAnnotation(DELETE.class);
    }

    public static void addLinks(AnnotatedElement element, String type, String rel) {
        AnnotationCreator linkResource = element.addAnnotation(LinkResource.class);
        linkResource.addValue("entityClassName", type);
        linkResource.addValue("rel", rel);
    }

    public static void addPath(AnnotatedElement element, String value) {
        element.addAnnotation(Path.class).addValue("value", value);
    }

    public static void addPathParam(AnnotatedElement element, String value) {
        element.addAnnotation(PathParam.class).addValue("value", value);
    }

    public static void addProduces(AnnotatedElement element, String... mediaTypes) {
        element.addAnnotation(Produces.class).addValue("value", mediaTypes);
    }

    public static void addConsumes(AnnotatedElement element, String... mediaTypes) {
        element.addAnnotation(Consumes.class).addValue("value", mediaTypes);
    }
}
