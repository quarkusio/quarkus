package io.quarkus.hibernate.orm.panache.common.runtime;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.SqmQuery;

import io.quarkus.hibernate.orm.panache.common.NestedProjectedClass;
import io.quarkus.hibernate.orm.panache.common.ProjectedFieldName;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;

public class CommonPanacheQueryImpl<Entity> {

    private interface NonThrowingCloseable extends AutoCloseable {
        @Override
        void close();
    }

    private static final NonThrowingCloseable NO_FILTERS = new NonThrowingCloseable() {
        @Override
        public void close() {
        }
    };

    private Object paramsArrayOrMap;
    /**
     * this is the HQL query expanded from the Panache-Query
     */
    private String query;
    /**
     * this is the original Panache-Query, if any (can be null)
     */
    private String originalQuery;
    /**
     * This is only used by the Spring Data JPA extension, due to Spring's Query annotation allowing a custom count query
     * See https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.at-query.native
     * Otherwise we do not use this, and rely on ORM to generate count queries
     */
    protected String customCountQueryForSpring;
    private String orderBy;
    private Session session;

    private Page page;
    private Long count;

    private Range range;

    private LockModeType lockModeType;
    private Map<String, Object> hints;

    private Map<String, Map<String, Object>> filters;
    private Class<?> projectionType;

    public CommonPanacheQueryImpl(Session session, String query, String originalQuery, String orderBy,
            Object paramsArrayOrMap) {
        this.session = session;
        this.query = query;
        this.originalQuery = originalQuery;
        this.orderBy = orderBy;
        this.paramsArrayOrMap = paramsArrayOrMap;
    }

    private CommonPanacheQueryImpl(CommonPanacheQueryImpl<?> previousQuery, String newQueryString,
            String customCountQueryForSpring,
            Class<?> projectionType) {
        this.session = previousQuery.session;
        this.query = newQueryString;
        this.customCountQueryForSpring = customCountQueryForSpring;
        this.orderBy = previousQuery.orderBy;
        this.paramsArrayOrMap = previousQuery.paramsArrayOrMap;
        this.page = previousQuery.page;
        this.count = previousQuery.count;
        this.range = previousQuery.range;
        this.lockModeType = previousQuery.lockModeType;
        this.hints = previousQuery.hints;
        this.filters = previousQuery.filters;
        this.projectionType = projectionType;
    }

    // Builder

    public <T> CommonPanacheQueryImpl<T> project(Class<T> type) {
        String selectQuery = query;
        if (PanacheJpaUtil.isNamedQuery(query)) {
            SelectionQuery<?> q = session.createNamedSelectionQuery(query.substring(1));
            selectQuery = getQueryString(q);
        }

        String lowerCasedTrimmedQuery = PanacheJpaUtil.trimForAnalysis(selectQuery);
        if (lowerCasedTrimmedQuery.startsWith("select new ")
                || lowerCasedTrimmedQuery.startsWith("select distinct new ")) {
            throw new PanacheQueryException("Unable to perform a projection on a 'select [distinct]? new' query: " + query);
        }

        // If the query starts with a select clause, we pass it on to ORM which can handle that via a projection type
        if (lowerCasedTrimmedQuery.startsWith("select ")) {
            // I think projections do not change the result count, so we can keep the custom count query
            return new CommonPanacheQueryImpl<>(this, query, customCountQueryForSpring, type);
        }

        // FIXME: this assumes the query starts with "FROM " probably?

        // build select clause with a constructor expression
        String selectClause = "SELECT " + getParametersFromClass(type, null);
        // I think projections do not change the result count, so we can keep the custom count query
        return new CommonPanacheQueryImpl<>(this, selectClause + selectQuery, customCountQueryForSpring, null);
    }

    private StringBuilder getParametersFromClass(Class<?> type, String parentParameter) {
        StringBuilder selectClause = new StringBuilder();
        // We use the first constructor that we found and use the parameter names,
        // so the projection class must have only one constructor,
        // and the application must be built with parameter names.
        // TODO: Maybe this should be improved some days ...
        Constructor<?> constructor = getConstructor(type); //type.getDeclaredConstructors()[0];
        selectClause.append("new ").append(type.getName()).append(" (");
        String parametersListStr = Stream.of(constructor.getParameters())
                .map(parameter -> getParameterName(type, parentParameter, parameter))
                .collect(Collectors.joining(","));
        selectClause.append(parametersListStr);
        selectClause.append(") ");
        return selectClause;
    }

    private Constructor<?> getConstructor(Class<?> type) {
        return type.getDeclaredConstructors()[0];
    }

    private String getParameterName(Class<?> parentType, String parentParameter, Parameter parameter) {
        String parameterName;
        // Check if constructor param is annotated with ProjectedFieldName
        if (hasProjectedFieldName(parameter)) {
            parameterName = getNameFromProjectedFieldName(parameter);
        } else if (!parameter.isNamePresent()) {
            throw new PanacheQueryException(
                    "Your application must be built with parameter names, this should be the default if" +
                            " using Quarkus project generation. Check the Maven or Gradle compiler configuration to include '-parameters'.");
        } else {
            // Check if class field with same parameter name exists and contains @ProjectFieldName annotation
            try {
                Field field = parentType.getDeclaredField(parameter.getName());
                parameterName = hasProjectedFieldName(field) ? getNameFromProjectedFieldName(field) : parameter.getName();
            } catch (NoSuchFieldException e) {
                parameterName = parameter.getName();
            }
        }
        // For nested classes, add parent parameter in parameterName
        parameterName = (parentParameter == null) ? parameterName : parentParameter.concat(".").concat(parameterName);
        // Test if the parameter is a nested Class that should be projected too.
        if (parameter.getType().isAnnotationPresent(NestedProjectedClass.class)) {
            Class<?> nestedType = parameter.getType();
            return getParametersFromClass(nestedType, parameterName).toString();
        } else {
            return parameterName;
        }
    }

    private boolean hasProjectedFieldName(AnnotatedElement annotatedElement) {
        return annotatedElement.isAnnotationPresent(ProjectedFieldName.class);
    }

    private String getNameFromProjectedFieldName(AnnotatedElement annotatedElement) {
        final String name = annotatedElement.getAnnotation(ProjectedFieldName.class).value();
        if (name.isEmpty()) {
            throw new PanacheQueryException("The annotation ProjectedFieldName must have a non-empty value.");
        }
        return name;
    }

    public void filter(String filterName, Map<String, Object> parameters) {
        if (filters == null)
            filters = new HashMap<>();
        filters.put(filterName, parameters);
    }

    public void page(Page page) {
        this.page = page;
        this.range = null; // reset the range to be able to switch from range to page
    }

    public void page(int pageIndex, int pageSize) {
        page(Page.of(pageIndex, pageSize));
    }

    public void nextPage() {
        checkPagination();
        page(page.next());
    }

    public void previousPage() {
        checkPagination();
        page(page.previous());
    }

    public void firstPage() {
        checkPagination();
        page(page.first());
    }

    public void lastPage() {
        checkPagination();
        page(page.index(pageCount() - 1));
    }

    public boolean hasNextPage() {
        checkPagination();
        return page.index < (pageCount() - 1);
    }

    public boolean hasPreviousPage() {
        checkPagination();
        return page.index > 0;
    }

    public int pageCount() {
        checkPagination();
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        return (int) Math.ceil((double) count / (double) page.size);
    }

    public Page page() {
        checkPagination();
        return page;
    }

    private void checkPagination() {
        if (page == null) {
            throw new UnsupportedOperationException("Cannot call a page related method, " +
                    "call page(Page) or page(int, int) to initiate pagination first");
        }
        if (range != null) {
            throw new UnsupportedOperationException("Cannot call a page related method in a ranged query, " +
                    "call page(Page) or page(int, int) to initiate pagination first");
        }
    }

    public void range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        // reset the page to its default to be able to switch from page to range
        this.page = null;
    }

    public void withLock(LockModeType lockModeType) {
        this.lockModeType = lockModeType;
    }

    public void withHint(String hintName, Object value) {
        if (hints == null) {
            hints = new HashMap<>();
        }
        hints.put(hintName, value);
    }

    // Results

    public long count() {
        if (count == null) {
            if (customCountQueryForSpring != null) {
                SelectionQuery<Long> countQuery = session.createSelectionQuery(customCountQueryForSpring, Long.class);
                if (paramsArrayOrMap instanceof Map)
                    AbstractJpaOperations.bindParameters(countQuery, (Map<String, Object>) paramsArrayOrMap);
                else
                    AbstractJpaOperations.bindParameters(countQuery, (Object[]) paramsArrayOrMap);
                try (NonThrowingCloseable c = applyFilters()) {
                    count = countQuery.getSingleResult();
                }
            } else {
                SelectionQuery<?> query = createBaseQuery();
                try (NonThrowingCloseable c = applyFilters()) {
                    count = query.getResultCount();
                }
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list() {
        SelectionQuery hibernateQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            return hibernateQuery.getResultList();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        SelectionQuery hibernateQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            return hibernateQuery.getResultStream();
        }
    }

    public <T extends Entity> T firstResult() {
        SelectionQuery hibernateQuery = createQuery(1);
        try (NonThrowingCloseable c = applyFilters()) {
            @SuppressWarnings("unchecked")
            List<T> list = hibernateQuery.getResultList();
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public <T extends Entity> Optional<T> firstResultOptional() {
        return Optional.ofNullable(firstResult());
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T singleResult() {
        SelectionQuery hibernateQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            return (T) hibernateQuery.getSingleResult();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Optional<T> singleResultOptional() {
        SelectionQuery hibernateQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            // Yes, there's a much nicer hibernateQuery.uniqueResultOptional() BUT
            //  it throws org.hibernate.NonUniqueResultException instead of a jakarta.persistence.NonUniqueResultException
            //  and at this point changing it would be a breaking change >_<
            return Optional.ofNullable((T) hibernateQuery.getSingleResultOrNull());
        }
    }

    private SelectionQuery createQuery() {
        SelectionQuery hibernateQuery = createBaseQuery();

        if (range != null) {
            hibernateQuery.setFirstResult(range.getStartIndex());
            // range is 0 based, so we add 1
            hibernateQuery.setMaxResults(range.getLastIndex() - range.getStartIndex() + 1);
        } else if (page != null) {
            hibernateQuery.setFirstResult(page.index * page.size);
            hibernateQuery.setMaxResults(page.size);
        } else {
            //no-op
        }

        return hibernateQuery;
    }

    private SelectionQuery createQuery(int maxResults) {
        SelectionQuery hibernateQuery = createBaseQuery();

        if (range != null) {
            hibernateQuery.setFirstResult(range.getStartIndex());
        } else if (page != null) {
            hibernateQuery.setFirstResult(page.index * page.size);
        } else {
            //no-op
        }
        hibernateQuery.setMaxResults(maxResults);

        return hibernateQuery;
    }

    @SuppressWarnings("unchecked")
    private SelectionQuery createBaseQuery() {
        SelectionQuery hibernateQuery;
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            hibernateQuery = session.createNamedSelectionQuery(namedQuery, projectionType);
        } else {
            try {
                hibernateQuery = session.createSelectionQuery(orderBy != null ? query + orderBy : query, projectionType);
            } catch (RuntimeException x) {
                throw NamedQueryUtil.checkForNamedQueryMistake(x, originalQuery);
            }
        }

        if (paramsArrayOrMap instanceof Map) {
            AbstractJpaOperations.bindParameters(hibernateQuery, (Map<String, Object>) paramsArrayOrMap);
        } else {
            AbstractJpaOperations.bindParameters(hibernateQuery, (Object[]) paramsArrayOrMap);
        }

        if (this.lockModeType != null) {
            hibernateQuery.setLockMode(lockModeType);
        }

        if (hints != null) {
            for (Map.Entry<String, Object> hint : hints.entrySet()) {
                hibernateQuery.setHint(hint.getKey(), hint.getValue());
            }
        }
        return hibernateQuery;
    }

    private NonThrowingCloseable applyFilters() {
        if (filters == null)
            return NO_FILTERS;
        for (Entry<String, Map<String, Object>> entry : filters.entrySet()) {
            Filter filter = session.enableFilter(entry.getKey());
            for (Entry<String, Object> paramEntry : entry.getValue().entrySet()) {
                if (paramEntry.getValue() instanceof Collection<?>) {
                    filter.setParameterList(paramEntry.getKey(), (Collection<?>) paramEntry.getValue());
                } else if (paramEntry.getValue() instanceof Object[]) {
                    filter.setParameterList(paramEntry.getKey(), (Object[]) paramEntry.getValue());
                } else {
                    filter.setParameter(paramEntry.getKey(), paramEntry.getValue());
                }
            }
            filter.validate();
        }
        return new NonThrowingCloseable() {
            @Override
            public void close() {
                for (Entry<String, Map<String, Object>> entry : filters.entrySet()) {
                    session.disableFilter(entry.getKey());
                }
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static String getQueryString(SelectionQuery hibernateQuery) {
        if (hibernateQuery instanceof SqmQuery) {
            return ((SqmQuery) hibernateQuery).getQueryString();
        } else if (hibernateQuery instanceof org.hibernate.query.Query) {
            // In theory we never use a Query, but who knows.
            return ((org.hibernate.query.Query) hibernateQuery).getQueryString();
        } else {
            throw new IllegalArgumentException("Unexpected Query class: '" + hibernateQuery.getClass().getName() + "', where '"
                    + SqmQuery.class.getName() + "' or '"
                    + org.hibernate.query.Query.class + "' is expected.");
        }
    }
}
