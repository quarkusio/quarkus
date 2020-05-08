package io.quarkus.panache.rest.common.deployment.methods;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.Collection;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.DataAccessImplementor;
import io.quarkus.panache.rest.common.deployment.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.utils.ResourceAnnotator;
import io.quarkus.panache.rest.common.deployment.utils.UrlImplementor;
import io.quarkus.panache.rest.common.runtime.hal.HalCollectionWrapper;

public final class ListHalMethodImplementor implements MethodImplementor {

    private final DataAccessImplementor dataAccessImplementor;

    private final String entityClassName;

    private final FieldDescriptor uriInfoField;

    public ListHalMethodImplementor(DataAccessImplementor dataAccessImplementor, String entityClassName,
            FieldDescriptor uriInfoField) {
        this.dataAccessImplementor = dataAccessImplementor;
        this.entityClassName = entityClassName;
        this.uriInfoField = uriInfoField;
    }

    /**
     * Implements HAL version of {@link PanacheCrudResource#list()}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Path("entities")
     *     &#64;Produces({"application/hal+json"})
     *     public HalCollectionWrapper listHal() {
     *         MultivaluedMap<String, String> params = uriInfo.getQueryParameters(true);
     *         if (params.containsKey("limit")) {
     *             return new HalCollectionWrapper(Entity.listAll(), Entity.class);
     *         } else {
     *             int limit;
     *             try {
     *                 limit = Integer.parseInt(params.getFirst("limit"));
     *                 if (limit <= 0) {
     *                     throw new BadRequestException("Limit cannot be negative");
     *                 }
     *             } catch (NumberFormatException var5) {
     *                 throw new BadRequestException("Limit has to be an integer");
     *             }
     *             PanacheQuery query = Entity.findAll();
     *             query.range(0, limit - 1);
     *             return return new HalCollectionWrapper(query.list(), Entity.class);
     *         }
     *     }
     * }
     * </pre>
     *
     * @param classCreator
     */
    @Override
    public void implement(ClassCreator classCreator) {
        MethodCreator methodCreator = classCreator.getMethodCreator("listHal", HalCollectionWrapper.class);
        ResourceAnnotator.addGet(methodCreator);
        ResourceAnnotator.addPath(methodCreator, UrlImplementor.getCollectionUrl(entityClassName));
        ResourceAnnotator.addProduces(methodCreator, ResourceAnnotator.APPLICATION_HAL_JSON);

        ResultHandle queryParameters = getQueryParameters(methodCreator);
        BranchResult ifHasLimit = methodCreator.ifTrue(hasLimit(methodCreator, queryParameters));

        // Has limit
        AssignableResultHandle limit = ifHasLimit.trueBranch().createVariable(int.class);
        setLimit(ifHasLimit.trueBranch(), queryParameters, limit);
        ifHasLimit.trueBranch().returnValue(wrapEntities(ifHasLimit.trueBranch(),
                dataAccessImplementor.list(ifHasLimit.trueBranch(), limit), entityClassName));
        // Does not have limit
        ifHasLimit.falseBranch().returnValue(wrapEntities(ifHasLimit.falseBranch(),
                dataAccessImplementor.listAll(ifHasLimit.falseBranch()), entityClassName));
        methodCreator.close();
    }

    private ResultHandle getQueryParameters(BytecodeCreator creator) {
        ResultHandle uriInfo = creator.readInstanceField(uriInfoField, creator.getThis());
        return creator.invokeInterfaceMethod(
                ofMethod(UriInfo.class, "getQueryParameters", MultivaluedMap.class, boolean.class),
                uriInfo, creator.load(true));
    }

    private ResultHandle hasLimit(BytecodeCreator creator, ResultHandle queryParameters) {
        return creator.invokeInterfaceMethod(ofMethod(MultivaluedMap.class, "containsKey", boolean.class, Object.class),
                queryParameters, creator.load("limit"));
    }

    private void setLimit(BytecodeCreator creator, ResultHandle queryParameters, AssignableResultHandle limitVariable) {
        TryBlock tryBlock = creator.tryBlock();
        ResultHandle stringLimit = tryBlock.invokeInterfaceMethod(
                ofMethod(MultivaluedMap.class, "getFirst", Object.class, Object.class),
                queryParameters, tryBlock.load("limit"));
        tryBlock.assign(limitVariable,
                tryBlock.invokeStaticMethod(ofMethod(Integer.class, "parseInt", int.class, String.class), stringLimit));
        tryBlock.ifLessEqualZero(limitVariable).trueBranch()
                .throwException(BadRequestException.class, "Limit cannot be negative");

        CatchBlockCreator catchBlockCreator = tryBlock.addCatch(NumberFormatException.class);
        catchBlockCreator.throwException(BadRequestException.class, "Limit has to be an integer");
        catchBlockCreator.close();
    }

    private ResultHandle wrapEntities(BytecodeCreator creator, ResultHandle entities, String entityType) {
        return creator.newInstance(MethodDescriptor.ofConstructor(HalCollectionWrapper.class, Collection.class, Class.class),
                entities, creator.loadClass(entityType));
    }
}
