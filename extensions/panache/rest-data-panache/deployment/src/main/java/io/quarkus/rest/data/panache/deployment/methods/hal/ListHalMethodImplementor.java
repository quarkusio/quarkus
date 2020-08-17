package io.quarkus.rest.data.panache.deployment.methods.hal;

import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.data.panache.deployment.PrivateFields.URI_INFO;
import static io.quarkus.rest.data.panache.deployment.PrivateMethods.IS_PAGED;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.methods.ListMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodMetadata;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.deployment.utils.PaginationImplementor;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;
import io.quarkus.rest.data.panache.runtime.hal.HalCollectionWrapper;

public final class ListHalMethodImplementor extends HalMethodImplementor {

    private static final String NAME = "listHal";

    /**
     * Implements HAL version of {@link RestDataResource#list()}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Path("")
     *     &#64;Produces({"application/hal+json"})
     *     public Response listHal() {
     *         if (this.isPaged()) {
     *            Page page = ...; // Extract page index and size from a UriInfo field and create a page instance.
     *            PanacheQuery query = Entity.findAll();
     *            query.page(page);
     *            List entities = query.list();
     *            // Get the page count, and build first, last, next, previous page instances
     *            HalCollectionWrapper wrapper = new HalCollectionWrapper(entities, Entity.class, "entities");
     *            // Add first, last, next and previous page URIs to the wrapper if they exist
     *            Response.ResponseBuilder responseBuilder = Response.status(200);
     *            responseBuilder.entity(wrapper);
     *            // Add headers with first, last, next and previous page URIs if they exist
     *            return responseBuilder.build();
     *         } else {
     *             List entities = Entity.listAll();
     *             return Response.ok(new HalCollectionWrapper(entities, Entity.class, "entities")).build();
     *         }
     *     }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodCreator methodCreator = classCreator.getMethodCreator(NAME, Response.class);
        addGetAnnotation(methodCreator);
        addPathAnnotation(methodCreator,
                propertiesAccessor.getPath(resourceInfo.getType(), getStandardMethodMetadata(resourceInfo)));
        addProducesAnnotation(methodCreator, MethodImplementor.APPLICATION_HAL_JSON);

        FieldDescriptor uriInfoField = of(methodCreator.getMethodDescriptor().getDeclaringClass(), URI_INFO.getName(),
                URI_INFO.getType());
        MethodDescriptor isPagedMethod = ofMethod(methodCreator.getMethodDescriptor().getDeclaringClass(), IS_PAGED.getName(),
                IS_PAGED.getType(), IS_PAGED.getParams());

        BranchResult isPaged = methodCreator.ifTrue(methodCreator.invokeVirtualMethod(isPagedMethod, methodCreator.getThis()));
        returnPaged(isPaged.trueBranch(), resourceInfo, uriInfoField);
        returnNotPaged(isPaged.falseBranch(), resourceInfo);
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getStandardMethodMetadata(RestDataResourceInfo resourceInfo) {
        return new MethodMetadata(ListMethodImplementor.NAME);
    }

    private void returnPaged(BytecodeCreator creator, RestDataResourceInfo resourceInfo, FieldDescriptor uriInfoField) {
        ResultHandle uriInfo = creator.readInstanceField(uriInfoField, creator.getThis());
        ResultHandle page = PaginationImplementor.getRequestPage(creator, uriInfo);
        ResultHandle pageCount = resourceInfo.getDataAccessImplementor().pageCount(creator, page);
        ResultHandle links = PaginationImplementor.getLinks(creator, uriInfo, page, pageCount);
        ResultHandle entities = resourceInfo.getDataAccessImplementor().findAll(creator, page);
        ResultHandle wrapper = wrapHalEntities(creator, entities, resourceInfo);
        creator.invokeVirtualMethod(ofMethod(HalCollectionWrapper.class, "addLinks", void.class, Link[].class), wrapper, links);

        creator.returnValue(ResponseImplementor.ok(creator, wrapper, links));
    }

    private void returnNotPaged(BytecodeCreator creator, RestDataResourceInfo resourceInfo) {
        ResultHandle entities = resourceInfo.getDataAccessImplementor().listAll(creator);
        creator.returnValue(ResponseImplementor.ok(creator, wrapHalEntities(creator, entities, resourceInfo)));
    }
}
