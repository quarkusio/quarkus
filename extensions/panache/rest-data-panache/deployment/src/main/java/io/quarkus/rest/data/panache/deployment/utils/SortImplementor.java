package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.common.Sort;

public final class SortImplementor {

    /**
     * Builds a Panache {@code Sort} instance based on the sort query parameters.
     * Sort query parameters could be either separated by comma or given as separate instances, e.g. '?sort=name,-age&sort=age'.
     * '-' sign before the field name indicates a descending order.
     *
     * @param creator Bytecode creator instance.
     * @param sortParams List of sort query strings
     * @return Panache Sort instance
     */
    public ResultHandle getSort(BytecodeCreator creator, ResultHandle sortParams) {
        ResultHandle sort = creator.invokeStaticMethod(
                ofMethod(Sort.class, "by", Sort.class, String[].class), creator.newArray(String.class, 0));
        ResultHandle fieldsIterator = creator.invokeInterfaceMethod(
                ofMethod(List.class, "iterator", Iterator.class), getSortFields(creator, sortParams));
        // Iterate through the sort fields
        BytecodeCreator loopCreator = creator.whileLoop(c -> iteratorHasNext(c, fieldsIterator)).block();
        ResultHandle field = loopCreator.invokeInterfaceMethod(
                ofMethod(Iterator.class, "next", Object.class), fieldsIterator);
        // Add the field to a sort object
        BranchResult isDescendingBranch = isDescendingField(loopCreator, field);
        addDescendingSortField(isDescendingBranch.trueBranch(), sort, field);
        addAscendingSortField(isDescendingBranch.falseBranch(), sort, field);
        return sort;
    }

    private void addAscendingSortField(BytecodeCreator creator, ResultHandle sort, ResultHandle field) {
        creator.invokeVirtualMethod(ofMethod(Sort.class, "and", Sort.class, String.class), sort, field);
    }

    private void addDescendingSortField(BytecodeCreator creator, ResultHandle sort, ResultHandle field) {
        ResultHandle fieldToAdd = creator.invokeVirtualMethod(
                ofMethod(String.class, "substring", String.class, int.class), field, creator.load(1));
        creator.invokeVirtualMethod(ofMethod(Sort.class, "and", Sort.class, String.class, Sort.Direction.class),
                sort, fieldToAdd, creator.load(Sort.Direction.Descending));
    }

    private BranchResult isDescendingField(BytecodeCreator creator, ResultHandle field) {
        return creator.ifTrue(creator.invokeVirtualMethod(
                ofMethod(String.class, "startsWith", boolean.class, String.class), field, creator.load("-")));
    }

    private ResultHandle getSortFields(BytecodeCreator creator, ResultHandle sortParamsList) {
        ResultHandle sortFieldsList = creator.newInstance(ofConstructor(LinkedList.class));
        ResultHandle sortParamsIterator = creator.invokeInterfaceMethod(
                ofMethod(List.class, "iterator", Iterator.class), sortParamsList);
        // Iterate through the sort query parameters
        BytecodeCreator loopCreator = creator.whileLoop(c -> iteratorHasNext(c, sortParamsIterator)).block();
        ResultHandle sortParam = loopCreator.invokeInterfaceMethod(
                ofMethod(Iterator.class, "next", Object.class), sortParamsIterator);
        ResultHandle extractedSortFields = extractSortFieldsFromParam(loopCreator, sortParam);
        loopCreator.invokeInterfaceMethod(
                ofMethod(List.class, "addAll", boolean.class, Collection.class), sortFieldsList, extractedSortFields);
        return sortFieldsList;
    }

    /**
     * Takes a single sort query parameter value, splits it by ',' and returns the result as a list.
     *
     * @param creator Bytecode creator instance
     * @param sortParam a single sort query parameter value (e.g. 'name' or 'name,-age')
     * @return A list of sort fields
     */
    private ResultHandle extractSortFieldsFromParam(BytecodeCreator creator, ResultHandle sortParam) {
        ResultHandle sortFieldsArray = creator.invokeVirtualMethod(
                ofMethod(String.class, "split", String[].class, String.class), sortParam, creator.load(","));
        return creator.invokeStaticMethod(ofMethod(Arrays.class, "asList", List.class, Object[].class), sortFieldsArray);
    }

    private BranchResult iteratorHasNext(BytecodeCreator creator, ResultHandle iterator) {
        return creator.ifTrue(creator.invokeInterfaceMethod(ofMethod(Iterator.class, "hasNext", boolean.class), iterator));
    }
}
