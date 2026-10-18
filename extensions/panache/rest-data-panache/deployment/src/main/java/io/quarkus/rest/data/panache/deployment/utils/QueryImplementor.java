package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public final class QueryImplementor {
    /**
     * Returns the name of the query or if it is not defined, then it will return a query built by the search parameters.
     *
     * <pre>
     * {@code
     * String query;
     * if (namedQuery != null) {
     *     query = "#" + namedQuery;
     * } else {
     *     query = String.join(" AND ", queryList);
     * }
     * }
     * </pre>
     *
     * @param creator a bytecode creator to be used for code generation.
     * @param namedQuery HQL query to list entities.
     * @param fieldValues fields query params.
     * @return query.
     */
    public AssignableResultHandle getQuery(BytecodeCreator creator, ResultHandle namedQuery,
            Map<String, ResultHandle> fieldValues) {
        ResultHandle queryList = creator.newInstance(ofConstructor(ArrayList.class));

        for (Map.Entry<String, ResultHandle> field : fieldValues.entrySet()) {
            String fieldName = field.getKey();
            String paramName = fieldName.replace(".", "__");
            ResultHandle fieldValueFromQuery = field.getValue();
            BytecodeCreator fieldValueFromQueryIsSet = creator.ifNotNull(fieldValueFromQuery).trueBranch();
            fieldValueFromQueryIsSet.invokeInterfaceMethod(ofMethod(List.class, "add", boolean.class, Object.class),
                    queryList, fieldValueFromQueryIsSet.load(fieldName + "=:" + paramName));
        }

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

        return query;
    }

    public ResultHandle getDataParams(BytecodeCreator creator, Map<String, ResultHandle> fieldValues) {
        ResultHandle dataParams = creator.newInstance(ofConstructor(HashMap.class));

        for (Map.Entry<String, ResultHandle> field : fieldValues.entrySet()) {
            String fieldName = field.getKey();
            String paramName = fieldName.replace(".", "__");
            ResultHandle fieldValueFromQuery = field.getValue();
            BytecodeCreator fieldValueFromQueryIsSet = creator.ifNotNull(fieldValueFromQuery).trueBranch();
            fieldValueFromQueryIsSet.invokeInterfaceMethod(
                    ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                    dataParams, fieldValueFromQueryIsSet.load(paramName), fieldValueFromQuery);
        }

        return dataParams;
    }
}
