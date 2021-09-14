package io.quarkus.spring.data.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.panache.common.Sort;

public class MethodNameParser {

    private static final String ALL_IGNORE_CASE = "AllIgnoreCase";
    private static final String IGNORE_CASE = "IgnoreCase";
    private static final String ORDER_BY = "OrderBy";

    private static final List<String> HANDLED_PROPERTY_OPERATIONS = Arrays.asList(
            "Is", "Equals",
            "IsNot", "Not",
            "IsNull", "Null",
            "IsNotNull", "NotNull",
            "IsBetween", "Between",
            "IsLessThan", "LessThan",
            "IsLessThanEqual", "LessThanEqual",
            "IsGreaterThan", "GreaterThan",
            "IsGreaterThanEqual", "GreaterThanEqual",
            "IsLike", "Like",
            "IsNotLike", "NotLike",
            "IsStartingWith", "StartingWith", "StartsWith",
            "IsEndingWith", "EndingWith", "EndsWith",
            "IsContaining", "Containing", "Contains",
            "Before", "IsBefore",
            "After", "IsAfter",
            "True", "False",
            "IsIn", "In",
            "IsNotIn", "NotIn",
            "IsEmpty", "Empty",
            "IsNotEmpty", "NotEmpty");

    private static final Set<String> STRING_LIKE_OPERATIONS = new HashSet<>(Arrays.asList(
            "IsLike", "Like",
            "IsNotLike", "NotLike",
            "IsStartingWith", "StartingWith", "StartsWith",
            "IsEndingWith", "EndingWith", "EndsWith",
            "IsContaining", "Containing", "Contains"));

    private static final Set<String> BOOLEAN_OPERATIONS = new HashSet<>(Arrays.asList("True", "False"));

    private final ClassInfo entityClass;
    private final IndexView indexView;
    private final List<ClassInfo> mappedSuperClassInfos;

    public MethodNameParser(ClassInfo entityClass, IndexView indexView) {
        this.entityClass = entityClass;
        this.indexView = indexView;
        this.mappedSuperClassInfos = getSuperClassInfos(indexView, entityClass);
    }

    public enum QueryType {
        SELECT,
        COUNT,
        EXISTS,
        DELETE;
    }

    public Result parse(MethodInfo methodInfo) {
        String methodName = methodInfo.name();
        ClassInfo repositoryClassInfo = methodInfo.declaringClass();
        String repositoryMethodDescription = methodName + " of Repository " + repositoryClassInfo;
        QueryType queryType = getType(methodName);
        if (queryType == null) {
            throw new UnableToParseMethodException("Method " + repositoryMethodDescription + " cannot be parsed");
        }

        int byIndex = methodName.indexOf("By");
        if ((byIndex == -1) || (byIndex + 2 >= methodName.length())) {
            throw new UnableToParseMethodException("Method " + repositoryMethodDescription + " cannot be parsed");
        }

        // handle 'Top' and 'First'
        Integer topCount = null;
        int firstIndex = methodName.indexOf("First");
        int topIndex = methodName.indexOf("Top");
        if ((firstIndex != -1) || (topIndex != -1)) {
            try {
                String topCountStr = methodName.substring(Math.max(firstIndex, topIndex), byIndex)
                        .replace("Top", "").replace("First", "");
                if (topCountStr.isEmpty()) {
                    topCount = 1;
                } else {
                    topCount = Integer.valueOf(topCountStr);
                }
            } catch (Exception e) {
                throw new UnableToParseMethodException(
                        "Unable to parse query with limiting results clause. Offending method is "
                                + repositoryMethodDescription + ".");
            }
        }
        if ((topCount != null) && (queryType != QueryType.SELECT)) {
            throw new UnableToParseMethodException(
                    "When 'Top' or 'First' is specified, the query must be a find query. Offending method is "
                            + repositoryMethodDescription + ".");
        }

        if (methodName.substring(0, byIndex).contains("Distinct")) {
            throw new UnableToParseMethodException(
                    "Distinct is not yet supported. Offending method is " + repositoryMethodDescription + ".");
        }

        // handle 'AllIgnoreCase'
        String afterByPart = methodName.substring(byIndex + 2);
        boolean allIgnoreCase = false;
        if (afterByPart.contains(ALL_IGNORE_CASE)) {
            allIgnoreCase = true;
            afterByPart = afterByPart.replace(ALL_IGNORE_CASE, "");
        }

        // handle the 'OrderBy' clause which is assumed to be at the end of the query
        Sort sort = null;
        if (containsLogicOperator(afterByPart, ORDER_BY)) {
            int orderByIndex = afterByPart.indexOf(ORDER_BY);
            if (orderByIndex + ORDER_BY.length() == afterByPart.length()) {
                throw new UnableToParseMethodException(
                        "A field must by supplied after 'OrderBy' . Offending method is " + repositoryMethodDescription + ".");
            }
            String afterOrderByPart = afterByPart.substring(orderByIndex + ORDER_BY.length());
            afterByPart = afterByPart.substring(0, orderByIndex);
            boolean ascending = true;
            if (afterOrderByPart.endsWith("Asc")) {
                ascending = true;
                afterOrderByPart = afterOrderByPart.replace("Asc", "");
            } else if (afterOrderByPart.endsWith("Desc")) {
                ascending = false;
                afterOrderByPart = afterOrderByPart.replace("Desc", "");
            }
            String orderField = lowerFirstLetter(afterOrderByPart);
            if (!entityContainsField(orderField)) {
                throw new UnableToParseMethodException(
                        "Field " + orderField
                                + " which was configured as the order field does not exist in the entity. Offending method is "
                                + repositoryMethodDescription + ".");
            }

            if (ascending) {
                sort = Sort.ascending(orderField);
            } else {
                sort = Sort.descending(orderField);
            }
        }

        List<String> parts = Collections.singletonList(afterByPart); // default when no 'And' or 'Or' exists
        boolean containsAnd = containsLogicOperator(afterByPart, "And");
        boolean containsOr = containsLogicOperator(afterByPart, "Or");
        if (containsAnd && containsOr) {
            throw new UnableToParseMethodException(
                    "'And' and 'Or' clauses cannot be mixed in a method name - Try specifying the Query with the @Query annotation. Offending method is "
                            + repositoryMethodDescription + ".");
        }
        if (containsAnd) {
            parts = Arrays.asList(afterByPart.split("And"));
        } else if (containsOr) {
            parts = Arrays.asList(afterByPart.split("Or"));
        }

        MutableReference<List<ClassInfo>> mappedSuperClassInfoRef = MutableReference.of(mappedSuperClassInfos);
        StringBuilder where = new StringBuilder();
        int paramsCount = 0;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            String fieldName;
            boolean ignoreCase = false;

            if (part.endsWith(IGNORE_CASE)) {
                ignoreCase = true;
                part = part.replace(IGNORE_CASE, "");
            }

            String operation = getFieldOperation(part);
            if (operation == null) { // no operation specified so we assume only the field was specified - something like findByLastname
                fieldName = lowerFirstLetter(part);
            } else {
                fieldName = lowerFirstLetter(part.replaceAll(operation, ""));
            }
            FieldInfo fieldInfo = getFieldInfo(fieldName, entityClass, mappedSuperClassInfoRef);
            if (fieldInfo == null) {
                StringBuilder fieldPathBuilder = new StringBuilder(fieldName.length() + 5);
                fieldInfo = resolveNestedField(repositoryMethodDescription, fieldName, fieldPathBuilder);
                fieldName = fieldPathBuilder.toString();
            }
            validateFieldWithOperation(operation, fieldInfo, fieldName, repositoryMethodDescription);
            if ((ignoreCase || allIgnoreCase) && !DotNames.STRING.equals(fieldInfo.type().name())) {
                throw new UnableToParseMethodException(
                        "IgnoreCase cannot be specified for field" + fieldInfo.name() + " because it is not a String type. "
                                + "Offending method is " + repositoryMethodDescription + ".");
            }

            if (where.length() > 0) {
                where.append(containsAnd ? " AND " : " OR ");
            }

            String upperPrefix = (ignoreCase || allIgnoreCase) ? "UPPER(" : "";
            String upperSuffix = (ignoreCase || allIgnoreCase) ? ")" : "";

            where.append(upperPrefix).append(fieldName).append(upperSuffix);
            if ((operation == null) || "Equals".equals(operation) || "Is".equals(operation)) {
                paramsCount++;
                where.append(" = ").append(upperPrefix).append("?").append(paramsCount).append(upperSuffix);
            } else {
                switch (operation) {
                    case "IsNot":
                    case "Not":
                        paramsCount++;
                        where.append(" <> ?").append(paramsCount);
                        break;
                    case "IsNull":
                    case "Null":
                        where.append(" IS null ");
                        break;
                    case "IsNotNull":
                    case "NotNull":
                        where.append(" IS NOT null ");
                        break;
                    case "Between":
                    case "IsBetween":
                        where.append(" BETWEEN ");
                        paramsCount++;
                        where.append("?").append(paramsCount).append(" AND ");
                        paramsCount++;
                        where.append("?").append(paramsCount);
                        break;
                    case "LessThan":
                    case "IsLessThan":
                    case "Before":
                    case "IsBefore":
                        paramsCount++;
                        where.append(" < ?").append(paramsCount);
                        break;
                    case "LessThanEqual":
                    case "IsLessThanEqual":
                        paramsCount++;
                        where.append(" <= ?").append(paramsCount);
                        break;
                    case "GreaterThan":
                    case "IsGreaterThan":
                    case "After":
                    case "IsAfter":
                        paramsCount++;
                        where.append(" > ?").append(paramsCount);
                        break;
                    case "GreaterThanEqual":
                    case "IsGreaterThanEqual":
                        paramsCount++;
                        where.append(" >= ?").append(paramsCount);
                        break;
                    case "Like":
                    case "IsLike":
                        paramsCount++;
                        where.append(" LIKE ?").append(paramsCount);
                        break;
                    case "NotLike":
                    case "IsNotLike":
                        paramsCount++;
                        where.append(" NOT LIKE ?").append(paramsCount);
                        break;
                    case "IsStartingWith":
                    case "StartingWith":
                    case "StartsWith":
                        paramsCount++;
                        where.append(" LIKE CONCAT(").append(upperPrefix).append("?").append(paramsCount).append(upperSuffix)
                                .append(", '%')");
                        break;
                    case "IsEndingWith":
                    case "EndingWith":
                    case "EndsWith":
                        paramsCount++;
                        where.append(" LIKE CONCAT('%', ").append(upperPrefix).append("?").append(paramsCount)
                                .append(upperSuffix).append(")");
                        break;
                    case "IsContaining":
                    case "Containing":
                    case "Contains":
                        paramsCount++;
                        where.append(" LIKE CONCAT('%', ").append(upperPrefix).append("?").append(paramsCount)
                                .append(upperSuffix).append(", '%')");
                        break;
                    case "True":
                    case "False":
                        where.append(" = ").append(operation.toLowerCase());
                        break;
                    case "IsIn":
                    case "In":
                        paramsCount++;
                        where.append(" IN ?").append(paramsCount);
                        break;
                    case "IsNotIn":
                    case "NotIn":
                        paramsCount++;
                        where.append(" NOT IN ?").append(paramsCount);
                        break;
                    case "IsEmpty":
                    case "Empty":
                        where.append(" IS EMPTY");
                        break;
                    case "IsNotEmpty":
                    case "NotEmpty":
                        where.append(" IS NOT EMPTY");
                        break;
                }
            }
        }

        String whereQuery = where.toString().isEmpty() ? "" : " WHERE " + where.toString();
        return new Result(entityClass, "FROM " + getEntityName() + whereQuery, queryType, paramsCount, sort,
                topCount);
    }

    /**
     * See:
     * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-property-expressions
     */
    private FieldInfo resolveNestedField(String repositoryMethodDescription, String fieldPathExpression,
            StringBuilder fieldPathBuilder) {

        String fieldNotResolvableMessage = "Entity " + this.entityClass + " does not contain a field named: "
                + fieldPathExpression + ". ";
        String offendingMethodMessage = "Offending method is " + repositoryMethodDescription + ".";

        ClassInfo parentClassInfo = this.entityClass;
        FieldInfo fieldInfo = null;

        int fieldStartIndex = 0;
        while (fieldStartIndex < fieldPathExpression.length()) {
            if (fieldPathExpression.charAt(fieldStartIndex) == '_') {
                fieldStartIndex++;
                if (fieldStartIndex >= fieldPathExpression.length()) {
                    throw new UnableToParseMethodException(fieldNotResolvableMessage + offendingMethodMessage);
                }
            }
            MutableReference<List<ClassInfo>> parentSuperClassInfos = new MutableReference<>();
            // the underscore character is treated as reserved character to manually define traversal points.
            int firstSeparator = fieldPathExpression.indexOf('_', fieldStartIndex);
            int fieldEndIndex = firstSeparator == -1 ? fieldPathExpression.length() : firstSeparator;
            while (fieldEndIndex >= fieldStartIndex) {
                String simpleFieldName = lowerFirstLetter(fieldPathExpression.substring(fieldStartIndex, fieldEndIndex));
                fieldInfo = getFieldInfo(simpleFieldName, parentClassInfo, parentSuperClassInfos);
                if (fieldInfo != null) {
                    break;
                }
                fieldEndIndex = previousPotentialFieldEnd(fieldPathExpression, fieldStartIndex, fieldEndIndex);
            }
            if (fieldInfo == null) {
                String detail = "";
                if (fieldStartIndex > 0) {
                    String notMatched = lowerFirstLetter(fieldPathExpression.substring(fieldStartIndex));
                    detail = "Can not resolve " + parentClassInfo + "." + notMatched + ". ";
                }
                throw new UnableToParseMethodException(
                        fieldNotResolvableMessage + detail + offendingMethodMessage);
            }
            if (fieldPathBuilder.length() > 0) {
                fieldPathBuilder.append('.');
            }
            fieldPathBuilder.append(fieldInfo.name());
            if (!isHibernateProvidedBasicType(fieldInfo.type().name())) {
                parentClassInfo = indexView.getClassByName(fieldInfo.type().name());
                if (parentClassInfo == null) {
                    throw new IllegalStateException(
                            "Entity class " + fieldInfo.type().name() + " referenced by "
                                    + this.entityClass + "." + fieldPathBuilder
                                    + " was not part of the Quarkus index. " + offendingMethodMessage);
                }
            }
            fieldStartIndex = fieldEndIndex;
        }

        return fieldInfo;
    }

    private int previousPotentialFieldEnd(String fieldName, int fieldStartIndex, int fieldEndIndexExclusive) {
        for (int i = fieldEndIndexExclusive - 1; i > fieldStartIndex; i--) {
            char c = fieldName.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Meant to be called with {@param operator} being {@code "And"} or {@code "Or"}
     * and returns {@code true} if the string contains the logical operator
     * and the next character is an uppercase character.
     * The reasoning is that if the next character is not uppercase,
     * then the operator string is just part of a word
     */
    private boolean containsLogicOperator(String str, String operatorStr) {
        int index = str.indexOf(operatorStr);
        if (index == -1) {
            return false;
        }
        if (str.length() < index + operatorStr.length() + 1) {
            return false;
        }
        return Character.isUpperCase(str.charAt(index + operatorStr.length()));
    }

    private void validateFieldWithOperation(String operation, FieldInfo fieldInfo, String fieldPath,
            String repositoryMethodDescription) {
        DotName fieldTypeDotName = fieldInfo.type().name();
        if (STRING_LIKE_OPERATIONS.contains(operation) && !DotNames.STRING.equals(fieldTypeDotName)) {
            throw new UnableToParseMethodException(
                    operation + " cannot be specified for field" + fieldPath + " because it is not a String type. "
                            + "Offending method is " + repositoryMethodDescription + ".");
        }

        if (BOOLEAN_OPERATIONS.contains(operation) && !DotNames.BOOLEAN.equals(fieldTypeDotName)
                && !DotNames.PRIMITIVE_BOOLEAN.equals(fieldTypeDotName)) {
            throw new UnableToParseMethodException(
                    operation + " cannot be specified for field" + fieldPath + " because it is not a boolean type. "
                            + "Offending method is " + repositoryMethodDescription + ".");
        }
    }

    private QueryType getType(String methodName) {
        if (methodName.startsWith("find") || methodName.startsWith("query") || methodName.startsWith("read")
                || methodName.startsWith("get")) {
            return QueryType.SELECT;
        }
        if (methodName.startsWith("count")) {
            return QueryType.COUNT;
        }
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return QueryType.DELETE;
        }
        if (methodName.startsWith("exists")) {
            return QueryType.EXISTS;
        }
        return null;
    }

    private String getFieldOperation(String part) {
        List<String> matches = new ArrayList<>();

        for (String handledPropertyOperation : HANDLED_PROPERTY_OPERATIONS) {
            if (part.endsWith(handledPropertyOperation)) {
                matches.add(handledPropertyOperation);
            }
        }

        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        // make sure we get the longest match
        matches.sort(Comparator.comparing(String::length).reversed());
        return matches.get(0);
    }

    private String lowerFirstLetter(String input) {
        if ((input == null) || input.isEmpty()) {
            return input;
        }
        if (input.length() == 1) {
            return input.toLowerCase();
        }

        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }

    private String getEntityName() {
        AnnotationInstance annotationInstance = entityClass.classAnnotation(DotNames.JPA_ENTITY);
        if (annotationInstance != null && annotationInstance.value("name") != null) {
            AnnotationValue annotationValue = annotationInstance.value("name");
            return annotationValue.asString().length() > 0 ? annotationValue.asString() : entityClass.simpleName();
        }
        return entityClass.simpleName();
    }

    private boolean entityContainsField(String fieldName) {
        if (entityClass.field(fieldName) != null) {
            return true;
        }

        for (ClassInfo superClass : mappedSuperClassInfos) {
            FieldInfo fieldInfo = superClass.field(fieldName);
            if (fieldInfo != null) {
                return true;
            }
        }
        return false;
    }

    private FieldInfo getFieldInfo(String fieldName, ClassInfo entityClass,
            MutableReference<List<ClassInfo>> superClassInfos) {
        FieldInfo fieldInfo = entityClass.field(fieldName);
        if (fieldInfo == null) {
            if (superClassInfos.isEmpty()) {
                superClassInfos.set(getSuperClassInfos(indexView, entityClass));
            }
            for (ClassInfo superClass : superClassInfos.get()) {
                fieldInfo = superClass.field(fieldName);
                if (fieldInfo != null) {
                    break;
                }
            }
        }
        return fieldInfo;
    }

    private List<ClassInfo> getSuperClassInfos(IndexView indexView, ClassInfo entityClass) {
        List<ClassInfo> mappedSuperClassInfoElements = new ArrayList<>(3);
        Type superClassType = entityClass.superClassType();
        while (superClassType != null && !superClassType.name().equals(DotNames.OBJECT)) {
            ClassInfo superClass = indexView.getClassByName(superClassType.name());
            if (superClass.classAnnotation(DotNames.JPA_MAPPED_SUPERCLASS) != null) {
                mappedSuperClassInfoElements.add(superClass);
            } else if (superClass.classAnnotation(DotNames.JPA_INHERITANCE) != null) {
                mappedSuperClassInfoElements.add(superClass);
            }

            if (superClassType.kind() == Kind.CLASS) {
                superClassType = superClass.superClassType();
            } else if (superClassType.kind() == Kind.PARAMETERIZED_TYPE) {
                ParameterizedType parameterizedType = superClassType.asParameterizedType();
                superClassType = parameterizedType.owner();
            }
        }
        return mappedSuperClassInfoElements;
    }

    private boolean isHibernateProvidedBasicType(DotName dotName) {
        return DotNames.HIBERNATE_PROVIDED_BASIC_TYPES.contains(dotName);
    }

    private static class MutableReference<T> {
        private T reference;

        public static <T> MutableReference<T> of(T reference) {
            return new MutableReference<>(reference);
        }

        public MutableReference() {
        }

        private MutableReference(T reference) {
            this.reference = reference;
        }

        public T get() {
            return reference;
        }

        public void set(T value) {
            this.reference = value;
        }

        public boolean isEmpty() {
            return reference == null;
        }
    }

    public static class Result {
        private final ClassInfo entityClass;
        private final String query;
        private final QueryType queryType;
        private final int paramCount;
        private final Sort sort;
        private final Integer topCount;

        public Result(ClassInfo entityClass, String query, QueryType queryType, int paramCount, Sort sort, Integer topCount) {
            this.entityClass = entityClass;
            this.query = query;
            this.queryType = queryType;
            this.paramCount = paramCount;
            this.sort = sort;
            this.topCount = topCount;
        }

        public ClassInfo getEntityClass() {
            return entityClass;
        }

        public String getQuery() {
            return query;
        }

        public QueryType getQueryType() {
            return queryType;
        }

        public int getParamCount() {
            return paramCount;
        }

        public Sort getSort() {
            return sort;
        }

        public Integer getTopCount() {
            return topCount;
        }
    }
}
