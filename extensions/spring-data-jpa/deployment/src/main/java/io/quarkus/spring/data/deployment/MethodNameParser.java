package io.quarkus.spring.data.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.Id;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

import io.quarkus.deployment.util.JandexUtil;
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
        String repositoryMethodDescription = "'" + methodName + "' of repository '" + repositoryClassInfo + "'";
        QueryType queryType = getType(methodName);
        String entityAlias = getEntityName().toLowerCase();
        // The SELECT clause is necessary after https://hibernate.atlassian.net/browse/HHH-18584
        String selectClause = queryType == QueryType.SELECT ? "SELECT " + entityAlias + " " : "";
        String fromClause = "FROM " + getEntityName() + " AS " + entityAlias;
        String joinClause = "";
        if (queryType == null) {
            throw new UnableToParseMethodException("Method " + repositoryMethodDescription
                    + " cannot be parsed. Did you forget to annotate the method with '@Query'?");
        }

        int byIndex = methodName.indexOf("By");
        if ((byIndex == -1) || (byIndex + 2 >= methodName.length())) {
            throw new UnableToParseMethodException("Method " + repositoryMethodDescription
                    + " cannot be parsed as there is no proper 'By' clause in the name.");
        }

        // handle 'Top' and 'First'
        Integer topCount = null;
        int minFirstOrTopIndex = Math.min(indexOfOrMaxValue(methodName, "First"), indexOfOrMaxValue(methodName, "Top"));
        // 'First' and 'Top' could be part of a field name, so we only consider them as part of a top query
        // if they are found before 'By'
        if (minFirstOrTopIndex < byIndex) {
            if (queryType != QueryType.SELECT) {
                throw new UnableToParseMethodException(
                        "When 'Top' or 'First' is specified, the query must be a find query. Offending method is "
                                + repositoryMethodDescription + ".");
            }
            try {
                String topCountStr = methodName.substring(minFirstOrTopIndex, byIndex)
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
        String[] partsArray = parts.toArray(new String[0]);
        //Spring supports mixing clauses 'And' and 'Or' together in method names
        if (containsAnd && containsOr) {
            List<String> words = splitAndIncludeRegex(afterByPart, "And", "Or");
            partsArray = words.toArray(new String[0]);
        } else if (containsAnd) {
            List<String> words = splitAndIncludeRegex(afterByPart, "And");
            partsArray = words.toArray(new String[0]);
        } else if (containsOr) {
            List<String> words = splitAndIncludeRegex(afterByPart, "Or");
            partsArray = words.toArray(new String[0]);
        }

        MutableReference<List<ClassInfo>> mappedSuperClassInfoRef = MutableReference.of(mappedSuperClassInfos);
        StringBuilder where = new StringBuilder();
        int paramsCount = 0;
        for (int i = 0; i < partsArray.length; i++) {
            String part = partsArray[i];
            if (part.isEmpty() || part.equals("And") || part.equals("Or")) {
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
                String topLevelFieldName = getTopLevelFieldName(fieldName);
                String childEntityAlias = topLevelFieldName;
                if (fieldInfo != null) {
                    //logic to find the field mapping with the parent (with entityClass)
                    FieldInfo relatedParentFieldInfo = indexView
                            .getClassByName(fieldInfo.declaringClass().name().toString()).fields()
                            .stream()
                            .filter(fi -> fi.type().name().equals(DotName.createSimple(entityClass.name().toString())))
                            .findFirst().orElse(null);
                    if (relatedParentFieldInfo != null) {
                        joinClause = " LEFT JOIN " + topLevelFieldName + " " + childEntityAlias + " ON "
                                + entityAlias + "." + getIdFieldInfo(entityClass).name() + " = "
                                + topLevelFieldName + "." + relatedParentFieldInfo.name() + "."
                                + getIdFieldInfo(entityClass).name();
                    } else {
                        // Fallback for cases where the relationship is not explicit
                        joinClause = " LEFT JOIN " + entityAlias + "." + topLevelFieldName + " " + childEntityAlias;
                    }

                }
            }
            validateFieldWithOperation(operation, fieldInfo, fieldName, repositoryMethodDescription);
            if ((ignoreCase || allIgnoreCase) && !DotNames.STRING.equals(fieldInfo.type().name())) {
                throw new UnableToParseMethodException(
                        "IgnoreCase cannot be specified for field" + fieldInfo.name() + " because it is not a String type. "
                                + "Offending method is " + repositoryMethodDescription + ".");
            }

            if (where.length() > 0) {
                if (containsAnd && partsArray[i - 1].equals("And"))
                    where.append(" AND ");
                if (containsOr && partsArray[i - 1].equals("Or"))
                    where.append(" OR ");
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
        return new Result(entityClass, selectClause + fromClause + joinClause + whereQuery, queryType, paramsCount, sort,
                topCount);
    }

    /**
     * Splits a given input string based on multiple regular expressions and includes each match in the result,
     * maintaining the order of parts and separators as they appear in the original string.
     *
     * <p>
     * This method allows you to provide multiple regex patterns that will be combined with an OR (`|`) operator,
     * so any match from the list of patterns will act as a delimiter. The result will include both the segments
     * of the input string that are between matches, as well as the matches themselves, in the order they appear.
     *
     * @param input the string to be split
     * @param regexes one or more regular expression patterns to use as delimiters
     * @return a list of strings representing the parts of the input string split by the specified regex patterns,
     *         with matches themselves included in the output list
     *
     *         <p>
     *         Example usage:
     *
     *         <pre>{@code
     * String input = "StatusAndCustomerIdAndColor";
     * List<String> result = splitAndIncludeRegex(input, "And");
     * // result: [Status, And, CustomerId, And, Color]
     * }</pre>
     */
    private List<String> splitAndIncludeRegex(String input, String... regexes) {
        List<String> result = new ArrayList<>();
        StringBuilder patternBuilder = new StringBuilder();

        // Create a pattern that combines all regex
        for (String regex : regexes) {
            if (patternBuilder.length() > 0) {
                patternBuilder.append("|");
            }
            // Add a limit word, \b, but adapted to camelCase (start or after a lowercase letter, end or followed by an uppercase letter)
            patternBuilder.append("(?<=[a-z])(").append(regex).append(")(?=[A-Z]|$)");
            patternBuilder.append("|(?<=^|[A-Z])(").append(regex).append(")(?=[A-Z]|$)");
        }
        Pattern pattern = Pattern.compile(patternBuilder.toString());
        Matcher matcher = pattern.matcher(input);

        int lastIndex = 0;
        while (matcher.find()) {
            // Add the part before the matching
            if (matcher.start() > lastIndex) {
                result.add(input.substring(lastIndex, matcher.start()));
            }
            // Add the regex
            result.add(matcher.group());
            lastIndex = matcher.end();
        }

        // Add the last part if exists
        if (lastIndex < input.length()) {
            result.add(input.substring(lastIndex));
        }

        return result;
    }

    /***
     * This method extracts the first part of an input string (up to the first dot), corresponding to the class containing a
     * field with the provided name.
     * Or returns the entire input if no dot is found.
     *
     * @param input: whole path of a nested field
     *        <p>
     *        Example usage:
     *
     *        <pre>{@code
     * String input = "customer.name";
     * List<String> result = getTopLevelFieldName(input);
     * // result: customer
     * }</pre>
     *
     */
    private String getTopLevelFieldName(String input) {
        if (input == null) {
            return null;
        }
        int dotIndex = input.indexOf('.');
        // if dot not found the whole string is returned
        if (dotIndex == -1) {
            return input;
        }
        return input.substring(0, dotIndex);
    }

    private int indexOfOrMaxValue(String methodName, String term) {
        int index = methodName.indexOf(term);
        return index != -1 ? index : Integer.MAX_VALUE;
    }

    /**
     * Resolves a nested field within an entity class based on a given field path expression.
     * This method traverses through the entity class and potentially its related classes,
     * identifying and returning the appropriate field. It handles complex field paths that may
     * include multiple levels of nested fields, separated by underscores ('_').
     *
     * See:
     * *
     * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-property-expressions
     *
     * @param repositoryMethodDescription A description of the repository method,
     *        typically used for error reporting.
     * @param fieldPathExpression The expression representing the path of the field within
     *        the entity class. Fields at different levels of nesting
     *        should be separated by underscores ('_').
     * @param fieldPathBuilder A StringBuilder used to construct and return the resolved field path.
     *        It will contain the fully qualified field path once the method completes.
     * @return The {@link FieldInfo} object representing the resolved field. If the field cannot be resolved,
     *         an exception is thrown.
     * @throws UnableToParseMethodException If the field cannot be resolved from the given
     *         field path expression, this exception is thrown
     *         with a detailed error message.
     * @throws IllegalStateException If the resolved entity class referenced by the field is not found
     *         in the Quarkus index, or if a typed field could not be resolved properly.
     */
    private FieldInfo resolveNestedField(String repositoryMethodDescription, String fieldPathExpression,
            StringBuilder fieldPathBuilder) {

        String fieldNotResolvableMessage = "Entity " + this.entityClass + " does not contain a field named: "
                + fieldPathExpression + ". ";
        String offendingMethodMessage = "Offending method is " + repositoryMethodDescription + ".";

        ClassInfo parentClassInfo = this.entityClass;
        FieldInfo fieldInfo = null;

        MutableReference<List<ClassInfo>> parentSuperClassInfos = new MutableReference<>();
        int fieldStartIndex = 0;
        ClassInfo parentFieldInfo = null;
        while (fieldStartIndex < fieldPathExpression.length()) {
            // The underscore character is treated as reserved character to manually define traversal points.
            // This means that path expression may have multiple levels separated by the '_' character. For example: person_address_city.
            if (fieldPathExpression.charAt(fieldStartIndex) == '_') {
                // See issue #34395
                // For resolving correctly nested fields added using '_' we need to get the previous fieldInfo which will be the class containing the field starting by '_' in this loop.
                DotName parentFieldInfoName;
                if (fieldInfo != null && fieldInfo.type().kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    parentFieldInfoName = fieldInfo.type().asParameterizedType().arguments().stream().findFirst().get().name();
                    parentFieldInfo = indexView.getClassByName(parentFieldInfoName);
                }
                fieldStartIndex++;
                if (fieldStartIndex >= fieldPathExpression.length()) {
                    throw new UnableToParseMethodException(fieldNotResolvableMessage + offendingMethodMessage);
                }
            }
            int firstSeparator = fieldPathExpression.indexOf('_', fieldStartIndex);
            int fieldEndIndex = firstSeparator == -1 ? fieldPathExpression.length() : firstSeparator;
            while (fieldEndIndex >= fieldStartIndex) {
                String fieldName = fieldPathExpression.substring(fieldStartIndex, fieldEndIndex);
                String simpleFieldName = lowerFirstLetter(fieldName);
                fieldInfo = getFieldInfo(simpleFieldName, parentFieldInfo == null ? parentClassInfo : parentFieldInfo,
                        parentSuperClassInfos);
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
                DotName parentClassName;
                boolean typed = false;
                if (fieldInfo.type().kind() == Type.Kind.TYPE_VARIABLE) {
                    typed = true;
                    parentClassName = getParentNameFromTypedFieldViaHierarchy(fieldInfo, mappedSuperClassInfos);
                } else if (fieldInfo.type().kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    parentClassName = fieldInfo.type().asParameterizedType().arguments().stream().findFirst().get().name();
                } else {
                    parentClassName = fieldInfo.type().name();
                }
                parentClassInfo = indexView.getClassByName(parentClassName);
                parentSuperClassInfos.set(null);
                if (parentClassInfo == null) {
                    throw new IllegalStateException(
                            "Entity class " + fieldInfo.type().name() + " referenced by "
                                    + this.entityClass + "." + fieldPathBuilder
                                    + " was not part of the Quarkus index"
                                    + (typed ? " or typed field could not be resolved properly. " : ". ")
                                    + offendingMethodMessage);
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

        // Check if the operator is at the beginning or preceded by capital letter.
        boolean startsCorrectly = (index == 0) || Character.isLowerCase(str.charAt(index - 1));

        // Check if the operator ends before the end or is followed by a capital letter.
        boolean endsCorrectly = (index + operatorStr.length() == str.length())
                || Character.isUpperCase(str.charAt(index + operatorStr.length()));

        return startsCorrectly && endsCorrectly;

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
        AnnotationInstance annotationInstance = entityClass.declaredAnnotation(DotNames.JPA_ENTITY);
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

    /**
     * Retrieves the first field in the given entity class that is annotated with `@Id`.
     *
     * @param entityClass the `ClassInfo` object representing the entity class.
     * @return the `FieldInfo` of the first field annotated with `@Id`.
     * @throws NoSuchElementException if no field with the `@Id` annotation is found.
     */
    private FieldInfo getIdFieldInfo(ClassInfo entityClass) {
        return entityClass.fields().stream()
                .filter(fieldInfo -> fieldInfo.hasAnnotation(DotName.createSimple(Id.class.getName()))).findFirst().get();
    }

    private List<ClassInfo> getSuperClassInfos(IndexView indexView, ClassInfo entityClass) {
        List<ClassInfo> mappedSuperClassInfoElements = new ArrayList<>(3);
        Type superClassType = entityClass.superClassType();
        while (superClassType != null && !superClassType.name().equals(DotNames.OBJECT)) {
            ClassInfo superClass = indexView.getClassByName(superClassType.name());
            if (superClass.declaredAnnotation(DotNames.JPA_MAPPED_SUPERCLASS) != null) {
                mappedSuperClassInfoElements.add(superClass);
            } else if (superClass.declaredAnnotation(DotNames.JPA_INHERITANCE) != null) {
                mappedSuperClassInfoElements.add(superClass);
            }

            superClassType = superClass.superClassType();
        }
        return mappedSuperClassInfoElements;
    }

    private boolean isHibernateProvidedBasicType(DotName dotName) {
        return DotNames.HIBERNATE_PROVIDED_BASIC_TYPES.contains(dotName);
    }

    // Tries to get the parent (name) of a field that is typed, e.g.:
    // class Foo<ID extends Serializable> { @EmbeddedId ID id; }
    // class Bar<ID extends SomeMoreSpecificBaseId> extends FOO<ID> { }
    // class Baz extends Bar<BazId> { }
    // @Embeddable class BazId extends SomeMoreSpecificBaseId { @Column String a; @Column String b; }
    //
    // Without this method, type of Foo.id would be Serializable and therefore Foo.id.a (or b) would not be found.
    //
    // Note: This method is lenient in that it doesn't throw exceptions aggressively when an assumption is not met.
    private DotName getParentNameFromTypedFieldViaHierarchy(FieldInfo fieldInfo, List<ClassInfo> parentSuperClassInfos) {
        // find in the hierarchy the position of the class the fieldInfo belongs to
        int superClassIndex = parentSuperClassInfos.indexOf(fieldInfo.declaringClass());
        if (superClassIndex == -1) {
            // field seems to belong to concrete entity class; no use narrowing it down via class hierarchy
            return fieldInfo.type().name();
        }
        TypeVariable typeVariable = fieldInfo.type().asTypeVariable();
        // entire hierarchy as list: entityClass, superclass of entityClass, superclass of the latter, ...
        List<ClassInfo> classInfos = new ArrayList<>();
        classInfos.add(entityClass);
        classInfos.addAll(parentSuperClassInfos.subList(0, superClassIndex + 1));
        // go down the hierarchy until ideally a concrete class is found that specifies the actual type of the field
        for (int i = classInfos.size() - 1; i > 0; i--) {
            ClassInfo currentClassInfo = classInfos.get(i);
            ClassInfo childClassInfo = classInfos.get(i - 1);
            int typeParameterIndex = currentClassInfo.typeParameters().indexOf(typeVariable);
            if (typeParameterIndex >= 0) {
                List<Type> resolveTypeParameters = JandexUtil.resolveTypeParameters(childClassInfo.name(),
                        currentClassInfo.name(), indexView);
                if (resolveTypeParameters.size() <= typeParameterIndex) {
                    // edge case; subclass without or with incomplete parameterization? raw types?
                    break;
                }
                Type type = resolveTypeParameters.get(typeParameterIndex);
                if (type.kind() == Type.Kind.TYPE_VARIABLE) {
                    // go on with the type variable from the child class (which is potentially more specific that the previous one)
                    typeVariable = type.asTypeVariable();
                } else if (type.kind() == Type.Kind.CLASS) {
                    // ideal outcome: concrete class, doesn't get more specific than this
                    return type.name();
                }
            }
        }
        // return the most specific type variable we were able to get
        return typeVariable.name();
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
