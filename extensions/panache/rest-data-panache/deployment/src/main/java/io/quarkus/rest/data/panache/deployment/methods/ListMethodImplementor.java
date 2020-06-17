package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.data.panache.deployment.PrivateFields.URI_INFO;
import static io.quarkus.rest.data.panache.deployment.PrivateMethods.IS_PAGED;

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
import io.quarkus.rest.data.panache.deployment.DataAccessImplementor;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.deployment.utils.PaginationImplementor;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;

public final class ListMethodImplementor extends StandardMethodImplementor {

    public static final String NAME = "list";

    private static final String REL = "list";

    /**
     * Implements {@link RestDataResource#list()}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Path("")
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "list",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Response list() {
     *         if (this.isPaged()) {
     *            Page page = ...; // Extract page index and size from a UriInfo field and create a page instance.
     *            PanacheQuery query = Entity.findAll();
     *            query.page(page);
     *            // Get the page count, and build first, last, next, previous page instances
     *            Response.ResponseBuilder responseBuilder = Response.status(200);
     *            responseBuilder.entity(query.list());
     *            // Add headers with first, last, next and previous page URIs if they exist
     *            return responseBuilder.build();
     *         } else {
     *             return Response.ok(Entity.listAll()).build();
     *         }
     *     }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        MethodCreator methodCreator = classCreator.getMethodCreator(methodMetadata.getName(), Response.class);
        addGetAnnotation(methodCreator);
        addPathAnnotation(methodCreator, propertiesAccessor.getPath(resourceInfo.getType(), methodMetadata));
        addProducesAnnotation(methodCreator, APPLICATION_JSON);
        addLinksAnnotation(methodCreator, resourceInfo.getEntityInfo().getType(), REL);

        FieldDescriptor uriInfoField = of(methodCreator.getMethodDescriptor().getDeclaringClass(), URI_INFO.getName(),
                URI_INFO.getType());
        MethodDescriptor isPagedMethod = ofMethod(methodCreator.getMethodDescriptor().getDeclaringClass(), IS_PAGED.getName(),
                IS_PAGED.getType(), IS_PAGED.getParams());

        BranchResult isPaged = methodCreator.ifTrue(methodCreator.invokeVirtualMethod(isPagedMethod, methodCreator.getThis()));
        returnPaged(isPaged.trueBranch(), resourceInfo.getDataAccessImplementor(), uriInfoField);
        returnNotPaged(isPaged.falseBranch(), resourceInfo.getDataAccessImplementor());
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getMethodMetadata(RestDataResourceInfo resourceInfo) {
        return new MethodMetadata(NAME);
    }

    private void returnPaged(BytecodeCreator creator, DataAccessImplementor dataAccessImplementor,
            FieldDescriptor uriInfoField) {
        ResultHandle uriInfo = creator.readInstanceField(uriInfoField, creator.getThis());
        ResultHandle page = PaginationImplementor.getRequestPage(creator, uriInfo);
        ResultHandle pageCount = dataAccessImplementor.pageCount(creator, page);
        ResultHandle links = PaginationImplementor.getLinks(creator, uriInfo, page, pageCount);
        ResultHandle entities = dataAccessImplementor.findAll(creator, page);

        creator.returnValue(ResponseImplementor.ok(creator, entities, links));
    }

    private void returnNotPaged(BytecodeCreator creator, DataAccessImplementor dataAccessImplementor) {
        creator.returnValue(ResponseImplementor.ok(creator, dataAccessImplementor.listAll(creator)));
    }
}
