package io.quarkus.resteasy.reactive.links.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.util.URLUtils;

import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.runtime.LinkInfo;
import io.quarkus.resteasy.reactive.links.runtime.LinksContainer;

final class LinksContainerFactory {

    private static final DotName REST_LINK_ANNOTATION = DotName.createSimple(RestLink.class.getName());

    private final IndexView index;

    LinksContainerFactory(IndexView index) {
        this.index = index;
    }

    /**
     * Find the resource methods that are marked with a {@link RestLink} annotations and add them to a links container.
     */
    LinksContainer getLinksContainer(List<ResourceClass> resourceClasses) {
        LinksContainer linksContainer = new LinksContainer();

        for (ResourceClass resourceClass : resourceClasses) {
            for (ResourceMethod resourceMethod : resourceClass.getMethods()) {
                MethodInfo resourceMethodInfo = getResourceMethodInfo(resourceClass, resourceMethod);
                AnnotationInstance restLinkAnnotation = resourceMethodInfo.annotation(REST_LINK_ANNOTATION);
                if (restLinkAnnotation != null) {
                    LinkInfo linkInfo = getLinkInfo(resourceClass, resourceMethod, resourceMethodInfo,
                            restLinkAnnotation);
                    linksContainer.put(linkInfo);
                }
            }
        }

        return linksContainer;
    }

    private LinkInfo getLinkInfo(ResourceClass resourceClass, ResourceMethod resourceMethod,
            MethodInfo resourceMethodInfo, AnnotationInstance restLinkAnnotation) {
        String rel = getAnnotationValue(restLinkAnnotation, "rel", resourceMethod.getName());
        String entityType = getAnnotationValue(restLinkAnnotation, "entityType", deductEntityType(resourceMethodInfo));
        String path = UriBuilder.fromPath(resourceClass.getPath()).path(resourceMethod.getPath()).toTemplate();
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

    /**
     * Find a {@link MethodInfo} for a given resource method
     */
    private MethodInfo getResourceMethodInfo(ResourceClass resourceClass, ResourceMethod resourceMethod) {
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(resourceClass.getClassName()));
        for (MethodInfo methodInfo : classInfo.methods()) {
            if (isSameMethod(methodInfo, resourceMethod)) {
                return methodInfo;
            }
        }
        throw new RuntimeException(String.format("Could not find method info for resource '%s.%s'",
                resourceClass.getClassName(), resourceMethod.getName()));
    }

    /**
     * Check if the given {@link MethodInfo} and {@link ResourceMethod} defined the same underlying method
     */
    private boolean isSameMethod(MethodInfo resourceMethodInfo, ResourceMethod resourceMethod) {
        if (!resourceMethodInfo.name().equals(resourceMethod.getName())) {
            return false;
        }
        if (resourceMethodInfo.parameters().size() != resourceMethod.getParameters().length) {
            return false;
        }

        List<Type> parameterTypes = resourceMethodInfo.parameters();
        MethodParameter[] parameters = resourceMethod.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (!parameterTypes.get(i).name().equals(DotName.createSimple(parameters[i].declaredType))) {
                return false;
            }
        }

        return true;
    }

    private String getAnnotationValue(AnnotationInstance annotationInstance, String name, String defaultValue) {
        AnnotationValue value = annotationInstance.value(name);
        if (value == null || value.asString().equals("")) {
            return defaultValue;
        }
        return value.asString();
    }
}
