package io.quarkus.resteasy.reactive.links.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.util.URLUtils;

import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.runtime.LinkInfo;
import io.quarkus.resteasy.reactive.links.runtime.LinksContainer;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveResourceMethodEntriesBuildItem;

final class LinksContainerFactory {

    /**
     * Find the resource methods that are marked with a {@link RestLink} annotations and add them to a links container.
     */
    LinksContainer getLinksContainer(List<ResteasyReactiveResourceMethodEntriesBuildItem.Entry> entries) {
        LinksContainer linksContainer = new LinksContainer();

        for (ResteasyReactiveResourceMethodEntriesBuildItem.Entry entry : entries) {
            MethodInfo resourceMethodInfo = entry.getMethodInfo();
            AnnotationInstance restLinkAnnotation = resourceMethodInfo.annotation(DotNames.REST_LINK_ANNOTATION);
            if (restLinkAnnotation != null) {
                LinkInfo linkInfo = getLinkInfo(entry.getResourceMethod(), resourceMethodInfo,
                        restLinkAnnotation, entry.getBasicResourceClassInfo().getPath());
                linksContainer.put(linkInfo);
            }
        }

        return linksContainer;
    }

    private LinkInfo getLinkInfo(ResourceMethod resourceMethod,
            MethodInfo resourceMethodInfo, AnnotationInstance restLinkAnnotation, String resourceClassPath) {
        String rel = getAnnotationValue(restLinkAnnotation, "rel", resourceMethod.getName());
        String entityType = getAnnotationValue(restLinkAnnotation, "entityType", deductEntityType(resourceMethodInfo));
        String path = UriBuilder.fromPath(resourceClassPath).path(resourceMethod.getPath()).toTemplate();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        Set<String> pathParameters = getPathParameters(path);

        return new LinkInfo(rel, entityType, path, pathParameters);
    }

    /**
     * If a method return type is parameterized and has a single argument (e.g. List), then use that argument as an
     * entity type. Otherwise, use the return type.
     */
    private String deductEntityType(MethodInfo methodInfo) {
        if (methodInfo.returnType().kind() == Type.Kind.PARAMETERIZED_TYPE) {
            if (methodInfo.returnType().asParameterizedType().arguments().size() == 1) {
                return methodInfo.returnType().asParameterizedType().arguments().get(0).name().toString();
            }
        }
        return methodInfo.returnType().name().toString();
    }

    /**
     * Extract parameters from a path string
     */
    private Set<String> getPathParameters(String path) {
        Set<String> names = new HashSet<>();
        URLUtils.parsePathParameters(path, names);
        Set<String> trimmedNames = new HashSet<>(names.size());
        for (String name : names) {
            trimmedNames.add(name.trim());
        }
        return trimmedNames;
    }

    private String getAnnotationValue(AnnotationInstance annotationInstance, String name, String defaultValue) {
        AnnotationValue value = annotationInstance.value(name);
        if (value == null || value.asString().equals("")) {
            return defaultValue;
        }
        return value.asString();
    }
}
