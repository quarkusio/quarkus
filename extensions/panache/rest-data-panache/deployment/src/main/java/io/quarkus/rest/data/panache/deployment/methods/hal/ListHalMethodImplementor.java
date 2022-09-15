package io.quarkus.rest.data.panache.deployment.methods.hal;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.data.panache.deployment.utils.PaginationImplementor.DEFAULT_PAGE_INDEX;
import static io.quarkus.rest.data.panache.deployment.utils.PaginationImplementor.DEFAULT_PAGE_SIZE;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.ofType;

import java.util.List;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.hal.HalCollectionWrapper;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.Constants;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.PaginationImplementor;
import io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator;
import io.quarkus.rest.data.panache.deployment.utils.SortImplementor;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;
import io.smallrye.mutiny.Uni;

public final class ListHalMethodImplementor extends HalMethodImplementor {

    private static final String METHOD_NAME = "listHal";

    private static final String RESOURCE_METHOD_NAME = "list";

    private static final String EXCEPTION_MESSAGE = "Failed to list the entities";

    private final PaginationImplementor paginationImplementor;

    private final SortImplementor sortImplementor = new SortImplementor();

    public ListHalMethodImplementor(boolean isResteasyClassic, boolean isReactivePanache) {
        super(isResteasyClassic, isReactivePanache);
        this.paginationImplementor = new PaginationImplementor();
    }

    /**
     * Generate HAL JAX-RS GET method.
     *
     * The RESTEasy Classic version exposes {@link RestDataResource#list(Page, Sort)} via HAL JAX-RS method.
     * Generated pseudocode with enabled pagination is shown below. If pagination is disabled pageIndex and pageSize
     * query parameters are skipped and null {@link Page} instance is used.
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Path("")
     *     &#64;Produces({"application/hal+json"})
     *     public Response listHal(@QueryParam("page") @DefaultValue("0") int pageIndex,
     *             &#64;QueryParam("size") @DefaultValue("20") int pageSize,
     *             &#64;QueryParam("sort") String sortQuery) {
     *         Page page = Page.of(pageIndex, pageSize);
     *         Sort sort = ...; // Build a sort instance by parsing a query param
     *         try {
     *             List<Entity> entities = resource.getAll(page, sort);
     *             // Get the page count, and build first, last, next, previous page instances
     *             HalCollectionWrapper wrapper = new HalCollectionWrapper(entities, Entity.class, "entities");
     *             // Add first, last, next and previous page URIs to the wrapper if they exist
     *             Response.ResponseBuilder responseBuilder = Response.status(200);
     *             responseBuilder.entity(wrapper);
     *             // Add headers with first, last, next and previous page URIs if they exist
     *             return responseBuilder.build();
     *         } catch (Throwable t) {
     *             throw new RestDataPanacheException(t);
     *         }
     *    }
     * }
     * </pre>
     *
     * The RESTEasy Reactive version exposes {@link io.quarkus.rest.data.panache.ReactiveRestDataResource#list(Page, Sort)}
     * and the generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Path("")
     *     &#64;Produces({"application/hal+json"})
     *     public Uni<Response> listHal(@QueryParam("page") @DefaultValue("0") int pageIndex,
     *             &#64;QueryParam("size") @DefaultValue("20") int pageSize,
     *             &#64;QueryParam("sort") String sortQuery) {
     *         Page page = Page.of(pageIndex, pageSize);
     *         Sort sort = ...; // Build a sort instance by parsing a query param
     *         resource.getAll(page, sort).map(entities -> {
     *             // Get the page count, and build first, last, next, previous page instances
     *             HalCollectionWrapper wrapper = new HalCollectionWrapper(entities, Entity.class, "entities");
     *             // Add first, last, next and previous page URIs to the wrapper if they exist
     *             Response.ResponseBuilder responseBuilder = Response.status(200);
     *             responseBuilder.entity(wrapper);
     *             // Add headers with first, last, next and previous page URIs if they exist
     *             return responseBuilder.build();
     *         }).onFailure().invoke(t -> throw new RestDataPanacheException(t));
     *    }
     * }
     * </pre>
     *
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        if (resourceProperties.isPaged()) {
            implementPaged(classCreator, resourceMetadata, resourceProperties, resourceField);
        } else {
            implementNotPaged(classCreator, resourceMetadata, resourceProperties, resourceField);
        }
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }

    private void implementPaged(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        // Method parameters: sort strings, page index, page size, uri info
        MethodCreator methodCreator = SignatureMethodCreator.getMethodCreator(METHOD_NAME, classCreator,
                isNotReactivePanache() ? ofType(Response.class) : ofType(Uni.class, resourceMetadata.getEntityType()),
                List.class, int.class, int.class, UriInfo.class);

        // Add method annotations
        addPathAnnotation(methodCreator, resourceProperties.getPath(RESOURCE_METHOD_NAME));
        addGetAnnotation(methodCreator);
        addProducesAnnotation(methodCreator, APPLICATION_HAL_JSON);
        addSortQueryParamValidatorAnnotation(methodCreator);
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(0), "sort");
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(1), "page");
        addDefaultValueAnnotation(methodCreator.getParameterAnnotations(1), Integer.toString(DEFAULT_PAGE_INDEX));
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(2), "size");
        addDefaultValueAnnotation(methodCreator.getParameterAnnotations(2), Integer.toString(DEFAULT_PAGE_SIZE));
        addContextAnnotation(methodCreator.getParameterAnnotations(3));

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle sortQuery = methodCreator.getMethodParam(0);
        ResultHandle sort = sortImplementor.getSort(methodCreator, sortQuery);
        ResultHandle pageIndex = methodCreator.getMethodParam(1);
        ResultHandle pageSize = methodCreator.getMethodParam(2);
        ResultHandle page = paginationImplementor.getPage(methodCreator, pageIndex, pageSize);
        ResultHandle uriInfo = methodCreator.getMethodParam(3);

        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
            ResultHandle pageCount = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), Constants.PAGE_COUNT_METHOD_PREFIX + RESOURCE_METHOD_NAME,
                            int.class, Page.class),
                    resource, page);
            ResultHandle links = paginationImplementor.getLinks(tryBlock, uriInfo, page, pageCount);
            ResultHandle entities = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, List.class, Page.class, Sort.class),
                    resource, page, sort);

            returnWrappedHalEntitiesWithLinks(tryBlock, resourceMetadata, resourceProperties, entities, links);

            tryBlock.close();
        } else {
            ResultHandle uniPageCount = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), Constants.PAGE_COUNT_METHOD_PREFIX + RESOURCE_METHOD_NAME,
                            Uni.class, Page.class),
                    resource, page);

            methodCreator.returnValue(UniImplementor.flatMap(methodCreator, uniPageCount, EXCEPTION_MESSAGE,
                    (pageCountBody, pageCount) -> {
                        ResultHandle links = paginationImplementor.getLinks(pageCountBody, uriInfo, page, pageCount);
                        ResultHandle uniEntities = pageCountBody.invokeVirtualMethod(
                                ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Uni.class, Page.class,
                                        Sort.class),
                                resource, page, sort);

                        pageCountBody.returnValue(UniImplementor.map(pageCountBody, uniEntities, EXCEPTION_MESSAGE,
                                (entitiesBody, entities) -> returnWrappedHalEntitiesWithLinks(entitiesBody, resourceMetadata,
                                        resourceProperties, entities, links)));
                    }));
        }

        methodCreator.close();
    }

    private void returnWrappedHalEntitiesWithLinks(BytecodeCreator body, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, ResultHandle entities, ResultHandle links) {

        ResultHandle wrapper = wrapHalEntities(body, entities, resourceMetadata.getEntityType(),
                resourceProperties.getHalCollectionName());

        body.invokeVirtualMethod(
                ofMethod(HalCollectionWrapper.class, "addLinks", void.class, Link[].class), wrapper, links);
        body.returnValue(responseImplementor.ok(body, wrapper, links));
    }

    private void implementNotPaged(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceFieldDescriptor) {
        MethodCreator methodCreator = SignatureMethodCreator.getMethodCreator(METHOD_NAME, classCreator,
                isNotReactivePanache() ? ofType(Response.class) : ofType(Uni.class, resourceMetadata.getEntityType()),
                List.class);

        // Add method annotations
        addPathAnnotation(methodCreator, resourceProperties.getPath(RESOURCE_METHOD_NAME));
        addGetAnnotation(methodCreator);
        addProducesAnnotation(methodCreator, APPLICATION_HAL_JSON);
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(0), "sort");

        ResultHandle sortQuery = methodCreator.getMethodParam(0);
        ResultHandle sort = sortImplementor.getSort(methodCreator, sortQuery);
        ResultHandle resource = methodCreator.readInstanceField(resourceFieldDescriptor, methodCreator.getThis());

        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, "Failed to list the entities");
            ResultHandle entities = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, List.class, Page.class, Sort.class),
                    resource, tryBlock.loadNull(), sort);

            returnWrappedHalEntities(tryBlock, resourceMetadata, resourceProperties, entities);

            tryBlock.close();
        } else {
            ResultHandle uniEntities = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Uni.class, Page.class, Sort.class),
                    resource, methodCreator.loadNull(), sort);

            methodCreator.returnValue(UniImplementor.map(methodCreator, uniEntities, EXCEPTION_MESSAGE,
                    (body, entities) -> returnWrappedHalEntities(body, resourceMetadata, resourceProperties, entities)));
        }

        methodCreator.close();
    }

    private void returnWrappedHalEntities(BytecodeCreator body, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties,
            ResultHandle entities) {
        ResultHandle wrapper = wrapHalEntities(body, entities, resourceMetadata.getEntityType(),
                resourceProperties.getHalCollectionName());
        body.returnValue(responseImplementor.ok(body, wrapper));
    }
}
