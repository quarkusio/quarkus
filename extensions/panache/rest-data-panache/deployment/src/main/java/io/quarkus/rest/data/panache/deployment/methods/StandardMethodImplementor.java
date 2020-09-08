package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.gizmo.FieldDescriptor.of;

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
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;

public abstract class StandardMethodImplementor implements MethodImplementor {

    @Override
    public void implement(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        if (propertiesAccessor.isExposed(resourceInfo.getType(), methodMetadata)) {
            implementInternal(classCreator, index, propertiesAccessor, resourceInfo);
        } else {
            NotExposedMethodImplementor implementor = new NotExposedMethodImplementor(methodMetadata);
            implementor.implement(classCreator, index, propertiesAccessor, resourceInfo);
        }
    }

    protected abstract void implementInternal(ClassCreator classCreator, IndexView index,
            MethodPropertiesAccessor propertiesAccessor, RestDataResourceInfo resourceInfo);

    protected abstract MethodMetadata getMethodMetadata(RestDataResourceInfo resourceInfo);

    protected void addTransactionalAnnotation(AnnotatedElement element) {
        element.addAnnotation(Transactional.class);
    }

    protected void addGetAnnotation(AnnotatedElement element) {
        element.addAnnotation(GET.class);
    }

    protected void addPostAnnotation(AnnotatedElement element) {
        element.addAnnotation(POST.class);
    }

    protected void addPutAnnotation(AnnotatedElement element) {
        element.addAnnotation(PUT.class);
    }

    protected void addDeleteAnnotation(AnnotatedElement element) {
        element.addAnnotation(DELETE.class);
    }

    protected void addLinksAnnotation(AnnotatedElement element, String type, String rel) {
        AnnotationCreator linkResource = element.addAnnotation(LinkResource.class);
        linkResource.addValue("entityClassName", type);
        linkResource.addValue("rel", rel);
    }

    protected void addPathAnnotation(AnnotatedElement element, String value) {
        element.addAnnotation(Path.class).addValue("value", value);
    }

    protected void addPathParamAnnotation(AnnotatedElement element, String value) {
        element.addAnnotation(PathParam.class).addValue("value", value);
    }

    protected void addProducesAnnotation(AnnotatedElement element, String... mediaTypes) {
        element.addAnnotation(Produces.class).addValue("value", mediaTypes);
    }

    protected void addConsumesAnnotation(AnnotatedElement element, String... mediaTypes) {
        element.addAnnotation(Consumes.class).addValue("value", mediaTypes);
    }

    protected ResultHandle getInstanceField(MethodCreator creator, String name, Class<?> type) {
        FieldDescriptor descriptor = of(creator.getMethodDescriptor().getDeclaringClass(), name, type);
        return creator.readInstanceField(descriptor, creator.getThis());
    }
}
