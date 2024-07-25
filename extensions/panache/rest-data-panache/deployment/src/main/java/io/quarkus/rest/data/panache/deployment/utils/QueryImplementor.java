package io.quarkus.rest.data.panache.deployment.utils;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

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
     * @param isList to get list query or count query.
     * @return query.
     */
    public AssignableResultHandle getQuery(BytecodeCreator creator, ResultHandle namedQuery,
            Map<String, ResultHandle> fieldValues, Boolean isList) {
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
                whenNamedQueryIsNotNull.load("#"), getSpecificQuery(whenNamedQueryIsNotNull, namedQuery, isList)));
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

    /**
     * Returns the list or counter query as specified. The list query is located before
     * the comma and on the left side is the count query.
     *
     * <pre>
     * {@code
     * var namedQueries = namedQuery.split(",", 2);
     * var listQuery = namedQueries.length >= 1 ? namedQueries[0] : "";
     * var countQuery = namedQueries.length == 2 ? namedQueries[1] : listQuery;
     *
     * return isList ? listQuery : countQuery;
     * }
     * </pre>
     *
     * @param creator a bytecode creator to be used for code generation.
     * @param namedQuery HQL query to list entities.
     * @param isList to get list query or count query.
     * @return list query or count query.
     */
    private ResultHandle getSpecificQuery(BytecodeCreator creator, ResultHandle namedQuery, Boolean isList) {
        ResultHandle namedQueries = creator.invokeVirtualMethod(ofMethod(String.class, "split", String[].class,
                String.class, int.class), namedQuery, creator.load(","), creator.load(2));
        ResultHandle lengthNamedQueries = creator.arrayLength(namedQueries);

        AssignableResultHandle specificQuery = creator.createVariable(String.class);

        if (isList) {
            BranchResult lengthGreaterThanOrEqualToOne = creator.ifIntegerGreaterEqual(lengthNamedQueries, creator.load(1));
            BytecodeCreator lengthGreaterThanOrEqualToOneTrue = lengthGreaterThanOrEqualToOne.trueBranch();
            lengthGreaterThanOrEqualToOneTrue.assign(specificQuery, lengthGreaterThanOrEqualToOneTrue
                    .readArrayValue(namedQueries, lengthGreaterThanOrEqualToOneTrue.load(0)));
            BytecodeCreator lengthGreaterThanOrEqualToOneFalse = lengthGreaterThanOrEqualToOne.falseBranch();
            lengthGreaterThanOrEqualToOneFalse.assign(specificQuery, lengthGreaterThanOrEqualToOneFalse.load(""));
        } else {
            BranchResult lengthGreaterThanOrEqualToTwo = creator.ifIntegerGreaterEqual(lengthNamedQueries, creator.load(2));
            BytecodeCreator lengthGreaterThanOrEqualToTwoTrue = lengthGreaterThanOrEqualToTwo.trueBranch();
            lengthGreaterThanOrEqualToTwoTrue.assign(specificQuery, lengthGreaterThanOrEqualToTwoTrue
                    .readArrayValue(namedQueries, lengthGreaterThanOrEqualToTwoTrue.load(1)));
            BytecodeCreator lengthGreaterThanOrEqualToTwoFalse = lengthGreaterThanOrEqualToTwo.falseBranch();
            lengthGreaterThanOrEqualToTwoFalse.assign(specificQuery, lengthGreaterThanOrEqualToTwoFalse
                    .readArrayValue(namedQueries, lengthGreaterThanOrEqualToTwoFalse.load(0)));
        }

        return specificQuery;
    }
}
