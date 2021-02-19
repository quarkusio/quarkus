package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.Iterator;
import java.util.List;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public final class CollectionImplementor {

    public ResultHandle iteratorFromList(BytecodeCreator creator, ResultHandle list) {
        return creator.invokeInterfaceMethod(
                ofMethod(List.class, "iterator", Iterator.class), list);
    }

    public ResultHandle getNext(BytecodeCreator loop, ResultHandle iterator, Class<?> clazz) {
        return loop.invokeInterfaceMethod(
                ofMethod(Iterator.class, "next", clazz), iterator);
    }

    public BranchResult iteratorHasNext(BytecodeCreator creator, ResultHandle iterator) {
        return creator.ifTrue(creator.invokeInterfaceMethod(ofMethod(Iterator.class, "hasNext", boolean.class), iterator));
    }
}
