package io.quarkus.hibernate.orm.panache.common.runtime;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Query;

import org.hibernate.Filter;
import org.hibernate.Session;

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
    protected String countQuery;
    private String orderBy;
    private EntityManager em;

    private Page page;
    private Long count;

    private Range range;

    private LockModeType lockModeType;
    private Map<String, Object> hints;

    private Map<String, Map<String, Object>> filters;

    public CommonPanacheQueryImpl(EntityManager em, String query, String originalQuery, String orderBy,
            Object paramsArrayOrMap) {
        this.em = em;
        this.query = query;
        this.originalQuery = originalQuery;
        this.orderBy = orderBy;
        this.paramsArrayOrMap = paramsArrayOrMap;
    }

    private CommonPanacheQueryImpl(CommonPanacheQueryImpl<?> previousQuery, String newQueryString, String countQuery) {
        this.em = previousQuery.em;
        this.query = newQueryString;
        this.countQuery = countQuery;
        this.orderBy = previousQuery.orderBy;
        this.paramsArrayOrMap = previousQuery.paramsArrayOrMap;
        this.page = previousQuery.page;
        this.count = previousQuery.count;
        this.range = previousQuery.range;
        this.lockModeType = previousQuery.lockModeType;
        this.hints = previousQuery.hints;
        this.filters = previousQuery.filters;
    }

    // Builder

    public <T> CommonPanacheQueryImpl<T> project(Class<T> type) {
        String selectQuery = query;
        if (PanacheJpaUtil.isNamedQuery(query)) {
            org.hibernate.query.Query q = (org.hibernate.query.Query) em.createNamedQuery(query.substring(1));
            selectQuery = q.getQueryString();
        }

        String lowerCasedTrimmedQuery = selectQuery.trim().replace('\n', ' ').replace('\r', ' ').toLowerCase();
        if (lowerCasedTrimmedQuery.startsWith("select new ")
                || lowerCasedTrimmedQuery.startsWith("select distinct new ")) {
            throw new PanacheQueryException("Unable to perform a projection on a 'select [distinct]? new' query: " + query);
        }

        // If the query starts with a select clause, we generate an HQL query
        // using the fields in the select clause:
        // Initial query: select e.field1, e.field2 from EntityClass e
        // New query: SELECT new org.acme.ProjectionClass(e.field1, e.field2) from EntityClass e
        if (lowerCasedTrimmedQuery.startsWith("select ")) {
            int endSelect = lowerCasedTrimmedQuery.indexOf(" from ");
            String trimmedQuery = selectQuery.trim().replace('\n', ' ').replace('\r', ' ');
            // 7 is the length of "select "
            String selectClause = trimmedQuery.substring(7, endSelect).trim();
            String from = trimmedQuery.substring(endSelect);
            StringBuilder newQuery = new StringBuilder("select ");
            // Handle select-distinct. HQL example: select distinct new org.acme.ProjectionClass...
            boolean distinctQuery = selectClause.toLowerCase().startsWith("distinct ");
            if (distinctQuery) {
                // 9 is the length of "distinct "
                selectClause = selectClause.substring(9).trim();
                newQuery.append("distinct ");
            }

            newQuery.append("new ").append(type.getName()).append("(").append(selectClause).append(")").append(from);
            return new CommonPanacheQueryImpl<>(this, newQuery.toString(), "select count(*) " + from);
        }

        // build select clause with a constructor expression
        String selectClause = "SELECT " + getParametersFromClass(type, null);
        return new CommonPanacheQueryImpl<>(this, selectClause + selectQuery,
                "select count(*) " + selectQuery);
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

    @SuppressWarnings("unchecked")
    public long count() {
        if (count == null) {
            String selectQuery = query;
            if (PanacheJpaUtil.isNamedQuery(query)) {
                org.hibernate.query.Query q = (org.hibernate.query.Query) em.createNamedQuery(query.substring(1));
                selectQuery = q.getQueryString();
            }

            Query countQuery = em.createQuery(countQuery(selectQuery));
            if (paramsArrayOrMap instanceof Map)
                AbstractJpaOperations.bindParameters(countQuery, (Map<String, Object>) paramsArrayOrMap);
            else
                AbstractJpaOperations.bindParameters(countQuery, (Object[]) paramsArrayOrMap);
            try (NonThrowingCloseable c = applyFilters()) {
                count = (Long) countQuery.getSingleResult();
            }
        }
        return count;
    }

    private String countQuery(String selectQuery) {
        if (countQuery != null) {
            return countQuery;
        }

        return PanacheJpaUtil.getCountQuery(selectQuery);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list() {
        Query jpaQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            return jpaQuery.getResultList();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        Query jpaQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            return jpaQuery.getResultStream();
        }
    }

    public <T extends Entity> T firstResult() {
        Query jpaQuery = createQuery(1);
        try (NonThrowingCloseable c = applyFilters()) {
            @SuppressWarnings("unchecked")
            List<T> list = jpaQuery.getResultList();
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public <T extends Entity> Optional<T> firstResultOptional() {
        return Optional.ofNullable(firstResult());
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T singleResult() {
        Query jpaQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            return (T) jpaQuery.getSingleResult();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Optional<T> singleResultOptional() {
        Query jpaQuery = createQuery(2);
        try (NonThrowingCloseable c = applyFilters()) {
            List<T> list = jpaQuery.getResultList();
            if (list.size() > 1) {
                throw new NonUniqueResultException();
            }

            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        }
    }

    private Query createQuery() {
        Query jpaQuery = createBaseQuery();

        if (range != null) {
            jpaQuery.setFirstResult(range.getStartIndex());
            // range is 0 based, so we add 1
            jpaQuery.setMaxResults(range.getLastIndex() - range.getStartIndex() + 1);
        } else if (page != null) {
            jpaQuery.setFirstResult(page.index * page.size);
            jpaQuery.setMaxResults(page.size);
        } else {
            //no-op
        }

        return jpaQuery;
    }

    private Query createQuery(int maxResults) {
        Query jpaQuery = createBaseQuery();

        if (range != null) {
            jpaQuery.setFirstResult(range.getStartIndex());
        } else if (page != null) {
            jpaQuery.setFirstResult(page.index * page.size);
        } else {
            //no-op
        }
        jpaQuery.setMaxResults(maxResults);

        return jpaQuery;
    }

    @SuppressWarnings("unchecked")
    private Query createBaseQuery() {
        Query jpaQuery;
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            jpaQuery = em.createNamedQuery(namedQuery);
        } else {
            try {
                jpaQuery = em.createQuery(orderBy != null ? query + orderBy : query);
            } catch (IllegalArgumentException x) {
                throw NamedQueryUtil.checkForNamedQueryMistake(x, originalQuery);
            }
        }

        if (paramsArrayOrMap instanceof Map) {
            AbstractJpaOperations.bindParameters(jpaQuery, (Map<String, Object>) paramsArrayOrMap);
        } else {
            AbstractJpaOperations.bindParameters(jpaQuery, (Object[]) paramsArrayOrMap);
        }

        if (this.lockModeType != null) {
            jpaQuery.setLockMode(lockModeType);
        }

        if (hints != null) {
            for (Map.Entry<String, Object> hint : hints.entrySet()) {
                jpaQuery.setHint(hint.getKey(), hint.getValue());
            }
        }
        return jpaQuery;
    }

    private NonThrowingCloseable applyFilters() {
        if (filters == null)
            return NO_FILTERS;
        Session session = em.unwrap(Session.class);
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
}
