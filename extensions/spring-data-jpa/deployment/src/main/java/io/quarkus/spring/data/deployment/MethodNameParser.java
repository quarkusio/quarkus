package io.quarkus.spring.data.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
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
        this.mappedSuperClassInfos = getMappedSuperClassInfos(indexView, entityClass);
    }

    public enum QueryType {
        SELECT,
        COUNT,
        EXISTS,
        DELETE;
    }

    public Result parse(MethodInfo methodInfo) {
        String methodName = methodInfo.name();
        QueryType queryType = getType(methodName);
        if (queryType == null) {
            throw new UnableToParseMethodException("Repository method " + methodName + " cannot be parsed");
        }

        int byIndex = methodName.indexOf("By");
        if ((byIndex == -1) || (byIndex + 2 >= methodName.length())) {
            throw new UnableToParseMethodException("Repository method " + methodName + " cannot be parsed");
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
                        "Unable to parse query with limiting results clause. Offending method is " + methodName);
            }
        }
        if ((topCount != null) && (queryType != QueryType.SELECT)) {
            throw new UnableToParseMethodException(
                    "When 'Top' or 'First' is specified, the query must be a find query. Offending method is " + methodName);
        }

        if (methodName.substring(0, byIndex).contains("Distinct")) {
            throw new UnableToParseMethodException(
                    "Distinct is not yet supported. Offending method is " + methodName);
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
                        "A field must by supplied after 'OrderBy' . Offending method is " + methodName);
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
                                + methodName);
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
                            + methodName);
        }
        if (containsAnd) {
            parts = Arrays.asList(afterByPart.split("And"));
        } else if (containsOr) {
            parts = Arrays.asList(afterByPart.split("Or"));
        }

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
            FieldInfo fieldInfo = getField(fieldName);
            if (fieldInfo == null) {
                String parsingExceptionMethod = "Entity " + entityClass + " does not contain a field named: " + fieldName +
                        ". " + "Offending method is " + methodName;

                // determine if we are trying to use a field of one of the associated entities

                int fieldEndIndex = -1;
                for (int i = 1; i < fieldName.length() - 1; i++) {
                    char c = fieldName.charAt(i);
                    if ((c >= 'A' && c <= 'Z') || c == '_') {
                        fieldEndIndex = i;
                        break;
                    }
                }

                if (fieldEndIndex == -1) {
                    throw new UnableToParseMethodException(parsingExceptionMethod);
                }

                int associatedEntityFieldStartIndex = fieldName.charAt(fieldEndIndex) == '_' ? fieldEndIndex + 1
                        : fieldEndIndex;
                if (associatedEntityFieldStartIndex >= fieldName.length() - 1) {
                    throw new UnableToParseMethodException(parsingExceptionMethod);
                }

                String simpleFieldName = fieldName.substring(0, fieldEndIndex);
                String associatedEntityFieldName = lowerFirstLetter(fieldName.substring(associatedEntityFieldStartIndex));
                fieldInfo = getField(simpleFieldName);
                if ((fieldInfo == null) || !(fieldInfo.type() instanceof ClassType)) {
                    throw new UnableToParseMethodException(parsingExceptionMethod);
                }

                ClassInfo associatedEntityClassInfo = indexView.getClassByName(fieldInfo.type().name());
                if (associatedEntityClassInfo == null) {
                    throw new IllegalStateException(
                            "Entity class " + fieldInfo.type().name() + " was not part of the Quarkus index");
                }
                FieldInfo associatedEntityClassField = getAssociatedEntityClassField(associatedEntityFieldName,
                        associatedEntityClassInfo);
                if (associatedEntityClassField == null) {
                    throw new UnableToParseMethodException(parsingExceptionMethod);
                }

                validateFieldWithOperation(operation, associatedEntityClassField, methodName);

                // set the fieldName to the proper JPQL expression
                fieldName = simpleFieldName + "." + associatedEntityFieldName;
            } else {
                validateFieldWithOperation(operation, fieldInfo, methodName);
            }
            if ((ignoreCase || allIgnoreCase) && !DotNames.STRING.equals(fieldInfo.type().name())) {
                throw new UnableToParseMethodException(
                        "IgnoreCase cannot be specified for field" + fieldInfo.name() + " of method "
                                + methodName + " because it is not a String type");
            }

            if (where.length() > 0) {
                where.append(containsAnd ? " AND " : " OR ");
            }

            String upperPrefix = (ignoreCase || allIgnoreCase) ? "UPPER(" : "";
            String upperSuffix = (ignoreCase || allIgnoreCase) ? ")" : "";

            // If the fieldName is not a field in the class and in camelcase format,
            // then split it as hierarchy of fields
            if (entityClass.field(fieldName) == null) {
                fieldName = handleFieldsHierarchy(fieldName, fieldInfo);
            }

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

    private String handleFieldsHierarchy(String fieldName, FieldInfo currentField) {
        StringBuilder finalName = new StringBuilder(fieldName);

        Set<String> childFields = new HashSet<>();

        childFields.addAll(entityClass.fields().stream()
                .map(FieldInfo::name)
                .collect(Collectors.toList()));

        // Collecting the current class fields
        ClassInfo currentClassInfo = indexView.getClassByName(currentField.type().name());

        if (currentClassInfo != null) {
            childFields.addAll(
                    currentClassInfo.fields()
                            .stream()
                            .map(FieldInfo::name)
                            .collect(Collectors.toList()));
        }

        // Collecting the inherited fields from the superclass of the actual class
        DotName superClassName = entityClass.superClassType().name();
        ClassInfo superClassInfo = indexView.getClassByName(superClassName);

        ClassInfo classByName;

        if (superClassName != null && superClassInfo != null && currentClassInfo != null &&
                currentClassInfo.superClassType() != null &&
                (classByName = indexView.getClassByName(currentClassInfo.superClassType().name())) != null) {

            childFields.addAll(superClassInfo.fields()
                    .stream()
                    .map(FieldInfo::name).collect(Collectors.toList()));

            childFields.addAll(classByName.fields()
                    .stream()
                    .map(FieldInfo::name).collect(Collectors.toList()));
        }

        // Collecting the inherited fields from the superclasses of the attributes
        if (currentClassInfo != null && currentClassInfo.superClassType() != null
                && (classByName = indexView.getClassByName(currentClassInfo.superClassType().name())) != null) {

            childFields.addAll(
                    classByName.fields()
                            .stream()
                            .map(FieldInfo::name).collect(Collectors.toList()));
        }

        // Building the fieldName from the members classes and their superclasses
        for (String fieldInf : childFields) {
            if (StringUtils.containsIgnoreCase(fieldName, fieldInf)) {
                String newValue = finalName.toString()
                        .replaceAll("(?i)" + fieldInf, lowerFirstLetter(fieldInf) + ".");
                finalName.delete(0, finalName.length());
                finalName.append(newValue);
            }
        }

        // In some cases, the built hierarchy is ending by a joining point. so we need to remove it
        if (finalName.toString().charAt(finalName.length() - 1) == '.') {
            fieldName = finalName.toString().replaceAll(".$", "");
        } else {
            fieldName = finalName.toString();
        }
        return fieldName;
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

    /**
     * Looks for the field in either the class itself or in a superclass that is annotated with @MappedSuperClass
     */
    private FieldInfo getAssociatedEntityClassField(String associatedEntityFieldName, ClassInfo associatedEntityClassInfo) {
        FieldInfo fieldInfo = associatedEntityClassInfo.field(associatedEntityFieldName);
        if (fieldInfo != null) {
            return fieldInfo;
        }
        if (DotNames.OBJECT.equals(associatedEntityClassInfo.superName())) {
            return null;
        }

        ClassInfo superClassInfo = indexView.getClassByName(associatedEntityClassInfo.superName());
        if (superClassInfo.classAnnotation(DotNames.JPA_MAPPED_SUPERCLASS) == null) {
            return null;
        }

        return getAssociatedEntityClassField(associatedEntityFieldName, superClassInfo);
    }

    private void validateFieldWithOperation(String operation, FieldInfo fieldInfo, String methodName) {
        DotName fieldTypeDotName = fieldInfo.type().name();
        if (STRING_LIKE_OPERATIONS.contains(operation) && !DotNames.STRING.equals(fieldTypeDotName)) {
            throw new UnableToParseMethodException(
                    operation + " cannot be specified for field" + fieldInfo.name() + " of method "
                            + methodName + " because it is not a String type");
        }

        if (BOOLEAN_OPERATIONS.contains(operation) && !DotNames.BOOLEAN.equals(fieldTypeDotName)
                && !DotNames.PRIMITIVE_BOOLEAN.equals(fieldTypeDotName)) {
            throw new UnableToParseMethodException(
                    operation + " cannot be specified for field" + fieldInfo.name() + " of method "
                            + methodName + " because it is not a boolean type");
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
        // TODO: not true?
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

    private FieldInfo getField(String fieldName) {
        // Before validating the fieldInfo,
        // we need to split the camelcase format and grab the first item
        FieldInfo fieldInfo = entityClass.field(fieldName);
        if (fieldInfo == null) {
            String[] camelCaseStrings = StringUtils.splitByCharacterTypeCamelCase(fieldName);
            fieldInfo = entityClass.field(camelCaseStrings[0]);
        }
        if (fieldInfo == null) {
            for (ClassInfo superClass : mappedSuperClassInfos) {
                fieldInfo = superClass.field(fieldName);
                if (fieldInfo != null) {
                    break;
                }
            }
        }
        return fieldInfo;
    }

    private List<ClassInfo> getMappedSuperClassInfos(IndexView indexView, ClassInfo entityClass) {
        List<ClassInfo> mappedSuperClassInfoElements = new ArrayList<>(3);
        Type superClassType = entityClass.superClassType();
        while (superClassType != null && !superClassType.name().equals(DotNames.OBJECT)) {
            ClassInfo superClass = indexView.getClassByName(superClassType.name());
            if (superClass.classAnnotation(DotNames.JPA_MAPPED_SUPERCLASS) != null) {
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
