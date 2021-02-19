package io.quarkus.spring.data.rest.deployment.crud;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.utils.CollectionImplementor;
import io.quarkus.rest.data.panache.deployment.utils.ReflectionImplementor;

public abstract class AbstractDataAccessImplementor {

    private final ReflectionImplementor reflectionImplementor = new ReflectionImplementor();

    private final CollectionImplementor collectionImplementor = new CollectionImplementor();

    protected void specificFieldUpdate(BytecodeCreator creator, ResultHandle foundEntity,
            ResultHandle inputEntity) {
        ResultHandle listOfFields = reflectionImplementor.getFieldsAsList(creator, inputEntity);
        ResultHandle iteratorFields = collectionImplementor.iteratorFromList(creator, listOfFields);
        BytecodeCreator loopCreator = creator.whileLoop(c -> collectionImplementor.iteratorHasNext(c, iteratorFields)).block();
        ResultHandle fieldNext = collectionImplementor.getNext(loopCreator, iteratorFields, Object.class);
        ResultHandle targetFieldName = reflectionImplementor.getFieldName(loopCreator, fieldNext);
        ResultHandle fieldOfFoundEntity = reflectionImplementor.getDeclaredField(loopCreator, foundEntity, targetFieldName);
        reflectionImplementor.setFieldAccessible(loopCreator, fieldOfFoundEntity, true);
        reflectionImplementor.setFieldAccessible(loopCreator, fieldNext, true);
        ResultHandle fieldInputValue = reflectionImplementor.getFieldValue(loopCreator, fieldNext, inputEntity);
        ResultHandle fieldFoundValue = reflectionImplementor.getFieldValue(loopCreator, fieldOfFoundEntity, foundEntity);
        BranchResult valueNotNull = loopCreator.ifNotNull(fieldInputValue);
        BranchResult valueNotEqual = valueNotNull.trueBranch().ifFalse(valueNotNull.trueBranch().invokeVirtualMethod(
                ofMethod(Object.class, "equals", boolean.class, Object.class), fieldInputValue,
                fieldFoundValue));
        reflectionImplementor.setFieldValue(valueNotEqual.trueBranch(), fieldOfFoundEntity, foundEntity, fieldInputValue);
        reflectionImplementor.setFieldAccessible(valueNotEqual.trueBranch(), fieldOfFoundEntity, false);
        reflectionImplementor.setFieldAccessible(valueNotEqual.trueBranch(), fieldNext, false);

        //for entities with superclass
        ResultHandle listOfSuperclassFields = reflectionImplementor.getSuperclassFieldsAsList(creator, inputEntity);
        ResultHandle iteratorSuperclassFields = collectionImplementor.iteratorFromList(creator, listOfSuperclassFields);
        BytecodeCreator loopSuperclassCreator = creator
                .whileLoop(c -> collectionImplementor.iteratorHasNext(c, iteratorSuperclassFields)).block();
        ResultHandle fieldSuperclassNext = collectionImplementor.getNext(loopSuperclassCreator, iteratorSuperclassFields,
                Object.class);
        ResultHandle targetSuperclassFieldName = reflectionImplementor.getFieldName(loopSuperclassCreator, fieldSuperclassNext);
        ResultHandle fieldOfFoundSuperclassEntity = reflectionImplementor.getDeclaredFieldOfSuperClass(loopSuperclassCreator,
                foundEntity, targetSuperclassFieldName);
        reflectionImplementor.setFieldAccessible(loopSuperclassCreator, fieldOfFoundSuperclassEntity, true);
        reflectionImplementor.setFieldAccessible(loopSuperclassCreator, fieldSuperclassNext, true);
        ResultHandle fieldSuperclassInputValue = reflectionImplementor.getFieldValue(loopSuperclassCreator, fieldSuperclassNext,
                inputEntity);
        ResultHandle fieldSuperclassFoundValue = reflectionImplementor.getFieldValue(loopSuperclassCreator,
                fieldOfFoundSuperclassEntity, foundEntity);
        BranchResult valueSuperclassNotNull = loopSuperclassCreator.ifNotNull(fieldSuperclassInputValue);
        BranchResult valueSuperclassNotEqual = valueSuperclassNotNull.trueBranch()
                .ifFalse(valueSuperclassNotNull.trueBranch().invokeVirtualMethod(
                        ofMethod(Object.class, "equals", boolean.class, Object.class), fieldSuperclassInputValue,
                        fieldSuperclassFoundValue));
        reflectionImplementor.setFieldValue(valueSuperclassNotEqual.trueBranch(), fieldOfFoundSuperclassEntity, foundEntity,
                fieldSuperclassInputValue);
        reflectionImplementor.setFieldAccessible(valueSuperclassNotEqual.trueBranch(), fieldOfFoundSuperclassEntity, false);
        reflectionImplementor.setFieldAccessible(valueSuperclassNotEqual.trueBranch(), fieldSuperclassNext, false);
    }

}
