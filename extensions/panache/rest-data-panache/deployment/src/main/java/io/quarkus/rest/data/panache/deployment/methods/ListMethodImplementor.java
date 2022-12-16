package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.arc.processor.DotNames.BOOLEAN;
import static io.quarkus.arc.processor.DotNames.CHARACTER;
import static io.quarkus.arc.processor.DotNames.DOUBLE;
import static io.quarkus.arc.processor.DotNames.FLOAT;
import static io.quarkus.arc.processor.DotNames.INTEGER;
import static io.quarkus.arc.processor.DotNames.LONG;
import static io.quarkus.arc.processor.DotNames.STRING;
import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.data.panache.deployment.utils.PaginationImplementor.DEFAULT_PAGE_INDEX;
import static io.quarkus.rest.data.panache.deployment.utils.PaginationImplementor.DEFAULT_PAGE_SIZE;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.ofType;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.param;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.jandex.Type;

import io.quarkus.deployment.Capabilities;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
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

public class ListMethodImplementor extends StandardMethodImplementor {

    private static final String METHOD_NAME = "list";

    private static final String RESOURCE_METHOD_NAME = "list";

    private static final String EXCEPTION_MESSAGE = "Failed to list the entities";

    private static final String REL = "list";

    private final PaginationImplementor paginationImplementor = new PaginationImplementor();
    private final SortImplementor sortImplementor = new SortImplementor();

    public ListMethodImplementor(Capabilities capabilities) {
        super(capabilities);
    }

    /**
     * Generate JAX-RS GET method.
     *
     * The RESTEasy Classic version exposes {@link RestDataResource#list(Page, Sort)}
     * and the generated pseudocode with enabled pagination is shown below. If pagination is disabled pageIndex and pageSize
     * query parameters are skipped and null {@link Page} instance is used.
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
     *     public Response list(@QueryParam("page") @DefaultValue("0") int pageIndex,
     *             &#64;QueryParam("size") @DefaultValue("20") int pageSize,
     *             &#64;QueryParam("sort") String sortQuery) {
     *         Page page = Page.of(pageIndex, pageSize);
     *         Sort sort = ...; // Build a sort instance by parsing a query param
     *         try {
     *             List<Entity> entities = resource.getAll(page, sort);
     *             // Get the page count, and build first, last, next, previous page instances
     *             Response.ResponseBuilder responseBuilder = Response.status(200);
     *             responseBuilder.entity(entities);
     *             // Add headers with first, last, next and previous page URIs if they exist
     *             return responseBuilder.build();
     *         } catch (Throwable t) {
     *             throw new RestDataPanacheException(t);
     *         }
     *     }
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
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "list",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Uni<Response> list(@QueryParam("page") @DefaultValue("0") int pageIndex,
     *             &#64;QueryParam("size") @DefaultValue("20") int pageSize,
     *             &#64;QueryParam("sort") String sortQuery) {
     *         Page page = Page.of(pageIndex, pageSize);
     *         Sort sort = ...; // Build a sort instance by parsing a query param
     *         try {
     *             return resource.getAll(page, sort).map(entities -> {
     *                // Get the page count, and build first, last, next, previous page instances
     *                Response.ResponseBuilder responseBuilder = Response.status(200);
     *                responseBuilder.entity(entities);
     *                // Add headers with first, last, next and previous page URIs if they exist
     *                return responseBuilder.build();
     *             });
     *
     *         } catch (Throwable t) {
     *             throw new RestDataPanacheException(t);
     *         }
     *     }
     * }
     * </pre>
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

    protected String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    protected void addProducesJsonAnnotation(AnnotatedElement element, ResourceProperties properties) {
        super.addProducesAnnotation(element, APPLICATION_JSON);
    }

    protected void returnValueWithLinks(BytecodeCreator creator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, ResultHandle value, ResultHandle links) {
        creator.returnValue(responseImplementor.ok(creator, value, links));
    }

    protected void returnValue(BytecodeCreator creator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, ResultHandle value) {
        creator.returnValue(responseImplementor.ok(creator, value));
    }

    private void implementPaged(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        // Method parameters: sort strings, page index, page size, uri info
        Collection<SignatureMethodCreator.Parameter> compatibleFieldsForQuery = getFieldsToQuery(resourceMetadata);
        List<SignatureMethodCreator.Parameter> parameters = new ArrayList<>();
        parameters.add(param("sort", List.class));
        parameters.add(param("page", int.class));
        parameters.add(param("size", int.class));
        parameters.add(param("uriInfo", UriInfo.class));
        parameters.add(param("namedQuery", String.class));
        parameters.addAll(compatibleFieldsForQuery);
        MethodCreator methodCreator = SignatureMethodCreator.getMethodCreator(getMethodName(), classCreator,
                isNotReactivePanache() ? ofType(Response.class) : ofType(Uni.class, resourceMetadata.getEntityType()),
                parameters);

        // Add method annotations
        addGetAnnotation(methodCreator);
        addPathAnnotation(methodCreator, resourceProperties.getPath(RESOURCE_METHOD_NAME));
        addProducesJsonAnnotation(methodCreator, resourceProperties);
        addLinksAnnotation(methodCreator, resourceMetadata.getEntityType(), REL);
        addMethodAnnotations(methodCreator, resourceProperties.getMethodAnnotations(RESOURCE_METHOD_NAME));
        addOpenApiResponseAnnotation(methodCreator, Response.Status.OK, resourceMetadata.getEntityType(), true);
        addSecurityAnnotations(methodCreator, resourceProperties);
        addSortQueryParamValidatorAnnotation(methodCreator);
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(0), "sort");
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(1), "page");
        addDefaultValueAnnotation(methodCreator.getParameterAnnotations(1), Integer.toString(DEFAULT_PAGE_INDEX));
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(2), "size");
        addDefaultValueAnnotation(methodCreator.getParameterAnnotations(2), Integer.toString(DEFAULT_PAGE_SIZE));
        addContextAnnotation(methodCreator.getParameterAnnotations(3));
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(4), "namedQuery");
        Map<String, ResultHandle> fieldValues = new HashMap<>();
        int index = 5;
        for (SignatureMethodCreator.Parameter param : compatibleFieldsForQuery) {
            addQueryParamAnnotation(methodCreator.getParameterAnnotations(index), param.getName());
            fieldValues.put(param.getName(), methodCreator.getMethodParam(index));
            index++;
        }

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle sortQuery = methodCreator.getMethodParam(0);
        ResultHandle sort = sortImplementor.getSort(methodCreator, sortQuery);
        ResultHandle pageIndex = methodCreator.getMethodParam(1);
        ResultHandle pageSize = methodCreator.getMethodParam(2);
        ResultHandle page = paginationImplementor.getPage(methodCreator, pageIndex, pageSize);
        ResultHandle uriInfo = methodCreator.getMethodParam(3);
        ResultHandle namedQuery = methodCreator.getMethodParam(4);

        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);

            ResultHandle pageCount = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), Constants.PAGE_COUNT_METHOD_PREFIX + RESOURCE_METHOD_NAME,
                            int.class, Page.class),
                    resource, page);

            ResultHandle links = paginationImplementor.getLinks(tryBlock, uriInfo, page, pageCount);
            ResultHandle entities = list(tryBlock, resourceMetadata, resource, page, sort, namedQuery, fieldValues);

            // Return response
            returnValueWithLinks(tryBlock, resourceMetadata, resourceProperties, entities, links);
            tryBlock.close();
        } else {
            ResultHandle uniPageCount = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), Constants.PAGE_COUNT_METHOD_PREFIX + RESOURCE_METHOD_NAME,
                            Uni.class, Page.class),
                    resource, page);

            methodCreator.returnValue(UniImplementor.flatMap(methodCreator, uniPageCount, EXCEPTION_MESSAGE,
                    (body, pageCount) -> {
                        ResultHandle pageCountAsInt = body.checkCast(pageCount, Integer.class);
                        ResultHandle links = paginationImplementor.getLinks(body, uriInfo, page, pageCountAsInt);
                        ResultHandle uniEntities = list(body, resourceMetadata, resource, page, sort, namedQuery, fieldValues);
                        body.returnValue(UniImplementor.map(body, uniEntities, EXCEPTION_MESSAGE,
                                (listBody, list) -> returnValueWithLinks(listBody, resourceMetadata, resourceProperties, list,
                                        links)));
                    }));
        }

        methodCreator.close();
    }

    private Collection<SignatureMethodCreator.Parameter> getFieldsToQuery(ResourceMetadata resourceMetadata) {
        return resourceMetadata.getFields().entrySet()
                .stream()
                .filter(e -> isFieldTypeCompatibleForQueryParam(e.getValue()))
                .map(e -> param(e.getKey(), e.getValue().name().toString()))
                .collect(Collectors.toList());
    }

    private void implementNotPaged(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceFieldDescriptor) {
        Collection<SignatureMethodCreator.Parameter> compatibleFieldsForQuery = getFieldsToQuery(resourceMetadata);
        List<SignatureMethodCreator.Parameter> parameters = new ArrayList<>();
        parameters.add(param("sort", List.class));
        parameters.add(param("namedQuery", String.class));
        parameters.addAll(compatibleFieldsForQuery);
        MethodCreator methodCreator = SignatureMethodCreator.getMethodCreator(getMethodName(), classCreator,
                isNotReactivePanache() ? ofType(Response.class) : ofType(Uni.class, resourceMetadata.getEntityType()),
                parameters);

        // Add method annotations
        addGetAnnotation(methodCreator);
        addPathAnnotation(methodCreator, resourceProperties.getPath(RESOURCE_METHOD_NAME));
        addProducesJsonAnnotation(methodCreator, resourceProperties);
        addLinksAnnotation(methodCreator, resourceMetadata.getEntityType(), REL);
        addMethodAnnotations(methodCreator, resourceProperties.getMethodAnnotations(RESOURCE_METHOD_NAME));
        addOpenApiResponseAnnotation(methodCreator, Response.Status.OK, resourceMetadata.getEntityType(), true);
        addSecurityAnnotations(methodCreator, resourceProperties);
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(0), "sort");
        addQueryParamAnnotation(methodCreator.getParameterAnnotations(1), "namedQuery");
        Map<String, ResultHandle> fieldValues = new HashMap<>();
        int index = 2;
        for (SignatureMethodCreator.Parameter param : compatibleFieldsForQuery) {
            addQueryParamAnnotation(methodCreator.getParameterAnnotations(index), param.getName());
            fieldValues.put(param.getName(), methodCreator.getMethodParam(index));
            index++;
        }

        ResultHandle sortQuery = methodCreator.getMethodParam(0);
        ResultHandle namedQuery = methodCreator.getMethodParam(1);
        ResultHandle sort = sortImplementor.getSort(methodCreator, sortQuery);
        ResultHandle resource = methodCreator.readInstanceField(resourceFieldDescriptor, methodCreator.getThis());

        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
            ResultHandle entities = list(tryBlock, resourceMetadata, resource, null, sort, namedQuery, fieldValues);
            returnValue(tryBlock, resourceMetadata, resourceProperties, entities);
            tryBlock.close();
        } else {
            ResultHandle uniEntities = list(methodCreator, resourceMetadata, resource, methodCreator.loadNull(), sort,
                    namedQuery, fieldValues);
            methodCreator.returnValue(UniImplementor.map(methodCreator, uniEntities, EXCEPTION_MESSAGE,
                    (body, entities) -> returnValue(body, resourceMetadata, resourceProperties, entities)));
        }

        methodCreator.close();
    }

    public ResultHandle list(BytecodeCreator creator, ResourceMetadata resourceMetadata, ResultHandle resource,
            ResultHandle page, ResultHandle sort, ResultHandle namedQuery, Map<String, ResultHandle> fieldValues) {

        ResultHandle dataParams = creator.newInstance(ofConstructor(HashMap.class));
        ResultHandle queryList = creator.newInstance(ofConstructor(ArrayList.class));
        for (Map.Entry<String, ResultHandle> field : fieldValues.entrySet()) {
            String fieldName = field.getKey();
            ResultHandle fieldValueFromQuery = field.getValue();
            BytecodeCreator fieldValueFromQueryIsSet = creator.ifNotNull(fieldValueFromQuery).trueBranch();
            fieldValueFromQueryIsSet.invokeInterfaceMethod(ofMethod(List.class, "add", boolean.class, Object.class),
                    queryList, fieldValueFromQueryIsSet.load(fieldName + "=:" + fieldName));
            fieldValueFromQueryIsSet.invokeInterfaceMethod(
                    ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                    dataParams, fieldValueFromQueryIsSet.load(fieldName), fieldValueFromQuery);
        }

        /**
         * String query;
         * if (namedQuery != null) {
         * query = "#" + namedQuery;
         * } else {
         * query = String.join(" AND ", queryList);
         * }
         */
        AssignableResultHandle query = creator.createVariable(String.class);
        BranchResult checkIfNamedQueryIsNull = creator.ifNull(namedQuery);
        BytecodeCreator whenNamedQueryIsNull = checkIfNamedQueryIsNull.trueBranch();
        BytecodeCreator whenNamedQueryIsNotNull = checkIfNamedQueryIsNull.falseBranch();
        whenNamedQueryIsNotNull.assign(query, whenNamedQueryIsNotNull.invokeVirtualMethod(
                ofMethod(String.class, "concat", String.class, String.class),
                whenNamedQueryIsNotNull.load("#"), namedQuery));
        whenNamedQueryIsNull.assign(query, whenNamedQueryIsNull.invokeStaticMethod(
                ofMethod(String.class, "join", String.class, CharSequence.class, Iterable.class),
                creator.load(" AND "), queryList));

        return creator.invokeVirtualMethod(
                ofMethod(resourceMetadata.getResourceClass(), "list", isNotReactivePanache() ? List.class : Uni.class,
                        Page.class, Sort.class, String.class, Map.class),
                resource, page == null ? creator.loadNull() : page, sort, query, dataParams);
    }

    private boolean isFieldTypeCompatibleForQueryParam(Type fieldType) {
        return fieldType.name().equals(STRING)
                || fieldType.name().equals(BOOLEAN)
                || fieldType.name().equals(CHARACTER)
                || fieldType.name().equals(DOUBLE)
                || fieldType.name().equals(FLOAT)
                || fieldType.name().equals(INTEGER)
                || fieldType.name().equals(LONG)
                || fieldType.kind() == Type.Kind.PRIMITIVE;
    }
}
