package io.quarkus.resteasy.reactive.links.deployment;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETABLE_FUTURE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_RESPONSE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.util.URLUtils;

import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.runtime.LinkInfo;
import io.quarkus.resteasy.reactive.links.runtime.LinksContainer;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveResourceMethodEntriesBuildItem;

final class LinksContainerFactory {

    private static final String LIST = "list";
    private static final String SELF = "self";
    private static final String REMOVE = "remove";
    private static final String UPDATE = "update";
    private static final String ADD = "add";

    /**
     * Find the resource methods that are marked with a {@link RestLink} annotations and add them to a {@link LinksContainer}.
     */
    LinksContainer getLinksContainer(List<ResteasyReactiveResourceMethodEntriesBuildItem.Entry> entries, IndexView index) {
        LinksContainer linksContainer = new LinksContainer();

        for (ResteasyReactiveResourceMethodEntriesBuildItem.Entry entry : entries) {
            MethodInfo resourceMethodInfo = entry.getMethodInfo();
            AnnotationInstance restLinkAnnotation = resourceMethodInfo.annotation(DotNames.REST_LINK_ANNOTATION);
            if (restLinkAnnotation != null) {
                LinkInfo linkInfo = getLinkInfo(entry.getResourceMethod(), resourceMethodInfo,
                        restLinkAnnotation, entry.getBasicResourceClassInfo().getPath(), index);
                linksContainer.put(linkInfo);
            }
        }

        return linksContainer;
    }

    private LinkInfo getLinkInfo(ResourceMethod resourceMethod, MethodInfo resourceMethodInfo,
            AnnotationInstance restLinkAnnotation, String resourceClassPath, IndexView index) {
        Type returnType = getNonAsyncReturnType(resourceMethodInfo.returnType());
        String rel = getAnnotationValue(restLinkAnnotation, "rel", deductRel(resourceMethod, returnType, index));
        String entityType = getAnnotationValue(restLinkAnnotation, "entityType", deductEntityType(returnType));
        String path = UriBuilder.fromPath(resourceClassPath).path(resourceMethod.getPath()).toTemplate();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        Set<String> pathParameters = getPathParameters(path);

        return new LinkInfo(rel, entityType, path, pathParameters);
    }

    /**
     * When the "rel" property is not set, it will be resolved as follows:
     * - "list" for GET methods returning a Collection.
     * - "self" for GET methods returning a non-Collection.
     * - "remove" for DELETE methods.
     * - "update" for PUT methods.
     * - "add" for POST methods.
     * <p>
     * Otherwise, it will return the method name.
     *
     * @param resourceMethod the resource method definition.
     * @return the deducted rel property.
     */
    private String deductRel(ResourceMethod resourceMethod, Type returnType, IndexView index) {
        String httpMethod = resourceMethod.getHttpMethod();
        boolean isCollection = isCollection(returnType, index);
        if (HttpMethod.GET.equals(httpMethod) && isCollection) {
            return LIST;
        } else if (HttpMethod.GET.equals(httpMethod)) {
            return SELF;
        } else if (HttpMethod.DELETE.equals(httpMethod)) {
            return REMOVE;
        } else if (HttpMethod.PUT.equals(httpMethod)) {
            return UPDATE;
        } else if (HttpMethod.POST.equals(httpMethod)) {
            return ADD;
        }

        return resourceMethod.getName();
    }

    /**
     * If a method return type is parameterized and has a single argument (e.g. List), then use that argument as an
     * entity type. Otherwise, use the return type.
     */
    private String deductEntityType(Type returnType) {
        if (returnType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            if (returnType.asParameterizedType().arguments().size() == 1) {
                return returnType.asParameterizedType().arguments().get(0).name().toString();
            }
        }
        return returnType.name().toString();
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

    private boolean isCollection(Type type, IndexView index) {
        if (type.kind() == Type.Kind.PRIMITIVE) {
            return false;
        }
        ClassInfo classInfo = index.getClassByName(type.name());
        if (classInfo == null) {
            return false;
        }
        return classInfo.interfaceNames().stream().anyMatch(DotName.createSimple(Collection.class.getName())::equals);
    }

    private Type getNonAsyncReturnType(Type returnType) {
        switch (returnType.kind()) {
            case ARRAY:
            case CLASS:
            case PRIMITIVE:
            case VOID:
                return returnType;
            case PARAMETERIZED_TYPE:
                // NOTE: same code in RuntimeResourceDeployment.getNonAsyncReturnType
                ParameterizedType parameterizedType = returnType.asParameterizedType();
                if (COMPLETION_STAGE.equals(parameterizedType.name())
                        || COMPLETABLE_FUTURE.equals(parameterizedType.name())
                        || UNI.equals(parameterizedType.name())
                        || MULTI.equals(parameterizedType.name())
                        || REST_RESPONSE.equals(parameterizedType.name())) {
                    return parameterizedType.arguments().get(0);
                }
                return returnType;
            default:
        }
        return returnType;
    }
}
