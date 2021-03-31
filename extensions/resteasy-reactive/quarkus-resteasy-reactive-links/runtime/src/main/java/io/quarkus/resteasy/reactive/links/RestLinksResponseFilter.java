package io.quarkus.resteasy.reactive.links;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Link;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;

public class RestLinksResponseFilter {

    private final RestLinksProvider linksProvider;

    public RestLinksResponseFilter(RestLinksProvider linksProvider) {
        this.linksProvider = linksProvider;
    }

    @ServerResponseFilter
    public void filter(ResourceInfo resourceInfo, ContainerResponseContext responseContext) {
        if (!(resourceInfo instanceof ResteasyReactiveResourceInfo)) {
            return;
        }
        for (Link link : getLinks((ResteasyReactiveResourceInfo) resourceInfo, responseContext)) {
            responseContext.getHeaders().add("Link", link);
        }
    }

    private Collection<Link> getLinks(ResteasyReactiveResourceInfo resourceInfo,
            ContainerResponseContext responseContext) {
        InjectRestLinks injectRestLinksAnnotation = getInjectRestLinksAnnotation(resourceInfo);
        if (injectRestLinksAnnotation == null) {
            return Collections.emptyList();
        }

        if (injectRestLinksAnnotation.value() == RestLinkType.INSTANCE && responseContext.hasEntity()) {
            return linksProvider.getInstanceLinks(responseContext.getEntity());
        }

        return linksProvider.getTypeLinks(getEntityType(resourceInfo, responseContext));
    }

    private InjectRestLinks getInjectRestLinksAnnotation(ResteasyReactiveResourceInfo resourceInfo) {
        if (resourceInfo.getMethodAnnotationNames().contains(InjectRestLinks.class.getName())) {
            for (Annotation annotation : resourceInfo.getAnnotations()) {
                if (annotation instanceof InjectRestLinks) {
                    return (InjectRestLinks) annotation;
                }
            }
        }
        if (resourceInfo.getClassAnnotationNames().contains(InjectRestLinks.class.getName())) {
            for (Annotation annotation : resourceInfo.getClassAnnotations()) {
                if (annotation instanceof InjectRestLinks) {
                    return (InjectRestLinks) annotation;
                }
            }
        }
        return null;
    }

    private Class<?> getEntityType(ResteasyReactiveResourceInfo resourceInfo,
            ContainerResponseContext responseContext) {
        for (Annotation annotation : resourceInfo.getAnnotations()) {
            if (annotation instanceof RestLink) {
                Class<?> entityType = ((RestLink) annotation).entityType();
                if (entityType != Object.class) {
                    return entityType;
                }
            }
        }
        return responseContext.getEntityClass();
    }
}
