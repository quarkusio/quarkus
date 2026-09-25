package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import org.hibernate.Filter;
import org.hibernate.SharedSessionContract;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.SqmQuery;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;
import io.quarkus.panache.hibernate.common.runtime.ProjectionConstructorUtil;

public class CommonPanacheQueryImpl<Entity> {

    /*
     * We use this complex caching mechanism to avoid recalculating projection queries
     * for recurring classes. In theory this gets stored in the Class object itself so
     * it is GCed when the class is disposed, so it auto-cleans itself. The extra
     * AtomicReference is as per Franz's advice, for a reason I did not understand.
     * We did verify that this improves allocation and cpu a lot, as it avoids
     * repeated usage of reflection and string building.
     */
    private final static ClassValue<AtomicReference<String>> ProjectionQueryCache = new ClassValue<>() {
        @Override
        protected AtomicReference<String> computeValue(Class<?> type) {
            return new AtomicReference<>();
        }
    };

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
    private Sort sort;
    private SharedSessionContract session;
    private Class<?> entityClass;

    private Long count;

    // We can only have one of page|range|keyedPage set at the same time, they're mutually exclusive
    private Page page;
    private Range range;
    private KeyedPage<?> keyedPage;
    private KeyedResultList<?> lastKeyedResult;

    private LockModeType lockModeType;
    private Map<String, Object> hints;

    private Map<String, Map<String, Object>> filters;
    private Class<?> projectionType;

    public CommonPanacheQueryImpl(SharedSessionContract session, Class<?> entityClass, String query, String originalQuery,
            Sort sort,
            Object paramsArrayOrMap) {
        this.session = session;
        this.entityClass = entityClass;
        this.query = query;
        this.originalQuery = originalQuery;
        this.sort = sort;
        this.paramsArrayOrMap = paramsArrayOrMap;
    }

    private CommonPanacheQueryImpl(CommonPanacheQueryImpl<?> previousQuery, String newQueryString,
            String customCountQueryForSpring,
            Class<?> projectionType) {
        this.session = previousQuery.session;
        this.entityClass = previousQuery.entityClass;
        this.query = newQueryString;
        this.customCountQueryForSpring = customCountQueryForSpring;
        this.sort = previousQuery.sort;
        this.paramsArrayOrMap = previousQuery.paramsArrayOrMap;
        this.page = previousQuery.page;
        this.count = previousQuery.count;
        this.range = previousQuery.range;
        this.keyedPage = previousQuery.keyedPage;
        this.lastKeyedResult = previousQuery.lastKeyedResult;
        this.lockModeType = previousQuery.lockModeType;
        this.hints = previousQuery.hints;
        this.filters = previousQuery.filters;
        this.projectionType = projectionType;
    }

    // Builder

    public CommonPanacheQueryImpl<Entity> sort(Sort sort) {
        this.sort = sort;
        return this;
    }

    public <T> CommonPanacheQueryImpl<T> project(Class<T> type) {
        return project(type, JoinType.INNER);
    }

    public <T> CommonPanacheQueryImpl<T> project(Class<T> type, JoinType joinType) {
        if (joinType == JoinType.RIGHT) {
            throw new PanacheQueryException(
                    "Only INNER and LEFT join types are supported for projections, got: " + joinType);
        }

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

        // When the caller opts into LEFT join semantics, try to generate explicit LEFT JOINs for the single-valued
        // associations navigated by the projection, so entities with a null association are still returned (with a null
        // value) instead of being filtered out by the implicit inner join. This only applies to the auto-generated
        // `FROM <entity>` query; anything else falls through to the default (implicit inner join) behavior below.
        if (joinType == JoinType.LEFT) {
            CommonPanacheQueryImpl<T> leftJoined = buildLeftJoinProjection(type, selectQuery);
            if (leftJoined != null) {
                return leftJoined;
            }
        }

        // FIXME: this assumes the query starts with "FROM " probably?

        // build select clause with a constructor expression
        AtomicReference<String> cachedProjection = ProjectionQueryCache.get(type);
        if (cachedProjection.get() == null) {
            cachedProjection.set("SELECT " + getParametersFromClass(type, null));
        }
        String selectClause = cachedProjection.get();
        // I think projections do not change the result count, so we can keep the custom count query
        return new CommonPanacheQueryImpl<>(this, selectClause + selectQuery, customCountQueryForSpring, null);
    }

    /**
     * Builds a projection query that navigates single-valued associations via explicit {@code LEFT JOIN}s.
     * <p>
     * Returns {@code null} (so the caller falls back to the default implicit-inner-join behavior) when the rewrite cannot
     * be applied safely: the query is not the auto-generated {@code FROM <entity>} form, the projection navigates no
     * association, or a path cannot be resolved / crosses a to-many association (which a LEFT JOIN would multiply).
     */
    private <T> CommonPanacheQueryImpl<T> buildLeftJoinProjection(Class<T> type, String selectQuery) {
        if (entityClass == null) {
            return null;
        }
        String entityName = PanacheJpaUtil.getEntityName(entityClass);
        // Only the auto-generated query (findAll / sort-only) is a bare `FROM <entity>` with no alias, where, or join.
        if (!selectQuery.trim().equalsIgnoreCase("FROM " + entityName)) {
            return null;
        }

        ManagedType<?> rootType;
        try {
            rootType = session.getFactory().getMetamodel().managedType(entityClass);
        } catch (RuntimeException e) {
            return null;
        }

        ProjectionJoins joins = new ProjectionJoins(rootType);
        String constructorExpression;
        String orderByClause;
        try {
            constructorExpression = getParametersFromClass(type, null, joins::qualifyPath).toString();
            // The generated joins add extra roots, so any sort must be qualified with the root alias to avoid an
            // "ambiguous unqualified attribute" error. We bake it into the query and clear the sort below.
            orderByClause = qualifiedOrderBy(sort, joins);
        } catch (UnsupportedProjectionJoinException e) {
            return null;
        }
        if (!joins.hasJoins()) {
            // The projection does not navigate any association, so the default behavior already returns every row.
            return null;
        }

        String newQuery = "SELECT " + constructorExpression + "FROM " + entityName + " " + ProjectionJoins.ROOT_ALIAS
                + joins.joinClause() + orderByClause;
        CommonPanacheQueryImpl<T> projected = new CommonPanacheQueryImpl<>(this, newQuery, customCountQueryForSpring, null);
        if (!orderByClause.isEmpty()) {
            // The sort is now part of the query; prevent it from being appended again (unqualified) at execution.
            projected.sort = null;
        }
        return projected;
    }

    /**
     * Renders a {@code Sort} as an {@code ORDER BY} whose columns are qualified with the projection's aliases, so it can
     * be embedded in a left-join projection query without triggering ambiguous-attribute errors.
     */
    private static String qualifiedOrderBy(Sort sort, ProjectionJoins joins) {
        if (sort == null || sort.getColumns().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        for (int i = 0; i < sort.getColumns().size(); i++) {
            Sort.Column column = sort.getColumns().get(i);
            if (i > 0) {
                sb.append(" , ");
            }
            String columnRef = joins.qualifyPath(column.getName());
            if (sort.isEscapingEnabled()) {
                columnRef = escapeQualifiedColumn(columnRef);
            }
            if (column.isIgnoreCase()) {
                sb.append("LOWER(").append(columnRef).append(")");
            } else {
                sb.append(columnRef);
            }
            if (column.getDirection() != Sort.Direction.Ascending) {
                sb.append(" DESC");
            }
            if (column.getNullPrecedence() != null) {
                sb.append(column.getNullPrecedence() == Sort.NullPrecedence.NULLS_FIRST ? " NULLS FIRST" : " NULLS LAST");
            }
        }
        return sb.toString();
    }

    /**
     * Backtick-escapes the attribute segments of an already alias-qualified path (e.g. {@code panache_e.name}), leaving
     * the leading alias untouched.
     */
    private static String escapeQualifiedColumn(String qualifiedPath) {
        String[] segments = qualifiedPath.split("\\.");
        StringBuilder sb = new StringBuilder(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            sb.append(".`").append(segments[i]).append('`');
        }
        return sb.toString();
    }

    private static StringBuilder getParametersFromClass(Class<?> type, String parentParameter) {
        return getParametersFromClass(type, parentParameter, UnaryOperator.identity());
    }

    private static StringBuilder getParametersFromClass(Class<?> type, String parentParameter,
            UnaryOperator<String> pathQualifier) {
        BiFunction<Class<?>, String, String> nestedProjectionBuilder = (nestedType, parameterName) -> getParametersFromClass(
                nestedType, parameterName, pathQualifier).toString();
        return new StringBuilder(
                ProjectionConstructorUtil.buildConstructorExpression(type, parentParameter, nestedProjectionBuilder,
                        pathQualifier));
    }

    /**
     * Thrown when a projection path cannot be safely turned into a LEFT JOIN (unresolvable attribute or a to-many
     * association, which would multiply rows). It makes {@link #buildLeftJoinProjection} bail out and fall back to the
     * default projection behavior.
     */
    private static final class UnsupportedProjectionJoinException extends RuntimeException {
        UnsupportedProjectionJoinException() {
            super(null, null, false, false);
        }
    }

    /**
     * Collects the explicit LEFT JOINs required by a projection and rewrites each bare entity path into an alias-qualified
     * path. Single-valued associations ({@code @ManyToOne}/{@code @OneToOne}) are joined; embeddable and basic segments
     * are navigated on the current alias without a join.
     */
    private static final class ProjectionJoins {

        static final String ROOT_ALIAS = "panache_e";

        private final ManagedType<?> rootType;
        // key: `<sourceAlias>.<memberPath>` of the association, value: the alias assigned to the joined entity
        private final Map<String, String> aliasByJoin = new LinkedHashMap<>();
        private final List<String> joins = new ArrayList<>();
        private int aliasCounter = 0;

        ProjectionJoins(ManagedType<?> rootType) {
            this.rootType = rootType;
        }

        boolean hasJoins() {
            return !joins.isEmpty();
        }

        String joinClause() {
            return String.join("", joins);
        }

        /**
         * Rewrites a bare projection path (e.g. {@code owner.name}) into an alias-qualified path (e.g.
         * {@code panache_e_j0.name}), registering any LEFT JOIN needed along the way.
         */
        String qualifyPath(String path) {
            String[] segments = path.split("\\.");
            ManagedType<?> currentType = rootType;
            String currentAlias = ROOT_ALIAS;
            // dotted path within the current alias accumulated while navigating embeddables (no join needed)
            String memberPath = "";

            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                Attribute<?, ?> attribute;
                try {
                    attribute = currentType.getAttribute(segment);
                } catch (IllegalArgumentException e) {
                    // Not a mapped attribute (function call, computed alias, ...): cannot rewrite safely.
                    throw new UnsupportedProjectionJoinException();
                }

                PersistentAttributeType attributeType = attribute.getPersistentAttributeType();
                boolean isLastSegment = i == segments.length - 1;

                if (attributeType == PersistentAttributeType.MANY_TO_ONE
                        || attributeType == PersistentAttributeType.ONE_TO_ONE) {
                    String source = memberPath.isEmpty() ? currentAlias + "." + segment
                            : currentAlias + "." + memberPath + "." + segment;
                    String alias = aliasByJoin.computeIfAbsent(source, s -> {
                        String newAlias = ROOT_ALIAS + "_j" + (aliasCounter++);
                        joins.add(" LEFT JOIN " + s + " " + newAlias);
                        return newAlias;
                    });
                    currentAlias = alias;
                    memberPath = "";
                    currentType = managedTypeOf(attribute);
                    if (currentType == null && !isLastSegment) {
                        throw new UnsupportedProjectionJoinException();
                    }
                } else if (attributeType == PersistentAttributeType.EMBEDDED) {
                    memberPath = memberPath.isEmpty() ? segment : memberPath + "." + segment;
                    currentType = managedTypeOf(attribute);
                    if (currentType == null && !isLastSegment) {
                        throw new UnsupportedProjectionJoinException();
                    }
                } else if (attributeType == PersistentAttributeType.BASIC) {
                    if (!isLastSegment) {
                        // A basic attribute cannot be navigated further.
                        throw new UnsupportedProjectionJoinException();
                    }
                    return memberPath.isEmpty() ? currentAlias + "." + segment
                            : currentAlias + "." + memberPath + "." + segment;
                } else {
                    // ONE_TO_MANY / MANY_TO_MANY / ELEMENT_COLLECTION: a LEFT JOIN over a collection multiplies rows.
                    throw new UnsupportedProjectionJoinException();
                }
            }

            // The path terminated on an association or embeddable (i.e. the projected value is that managed type itself).
            return memberPath.isEmpty() ? currentAlias : currentAlias + "." + memberPath;
        }

        private static ManagedType<?> managedTypeOf(Attribute<?, ?> attribute) {
            if (attribute instanceof SingularAttribute) {
                Type<?> type = ((SingularAttribute<?, ?>) attribute).getType();
                if (type instanceof ManagedType) {
                    return (ManagedType<?>) type;
                }
            }
            return null;
        }
    }

    public void filter(String filterName, Map<String, Object> parameters) {
        if (filters == null)
            filters = new HashMap<>();
        filters.put(filterName, parameters);
    }

    public void page(Page page) {
        this.page = page;
        this.range = null;
        this.keyedPage = null;
        this.lastKeyedResult = null;
    }

    public void page(int pageIndex, int pageSize) {
        page(Page.of(pageIndex, pageSize));
    }

    @SuppressWarnings("unchecked")
    public void cursor(int pageIndex, int pageSize) {
        if (sort == null || sort.getColumns().isEmpty()) {
            throw new UnsupportedOperationException(
                    "Cannot use cursor-based pagination without sort criteria: use find(entityClass, query, sort) or findAll(entityClass, sort)");
        }
        List orders = PanacheJpaUtil.toHibernateOrders(entityClass, sort);
        this.keyedPage = org.hibernate.query.Page.page(pageSize, pageIndex).keyedBy(orders);
        this.lastKeyedResult = null;
        this.page = null;
        this.range = null;
    }

    public void nextPage() {
        checkPagination();
        if (keyedPage != null) {
            if (lastKeyedResult == null) {
                throw new UnsupportedOperationException(
                        "Cannot call nextPage() before fetching results with list()");
            }
            keyedPage = lastKeyedResult.getNextPage();
            lastKeyedResult = null;
        } else {
            page(page.next());
        }
    }

    public void previousPage() {
        checkPagination();
        if (keyedPage != null) {
            if (lastKeyedResult == null) {
                throw new UnsupportedOperationException(
                        "Cannot call previousPage() before fetching results with list()");
            }
            KeyedPage<?> prev = lastKeyedResult.getPreviousPage();
            if (prev != null) {
                keyedPage = prev;
            }
            lastKeyedResult = null;
        } else {
            page(page.previous());
        }
    }

    @SuppressWarnings("unchecked")
    public void firstPage() {
        checkPagination();
        if (keyedPage != null) {
            keyedPage = keyedPage.getPage().first().keyedBy((List) keyedPage.getKeyDefinition());
            lastKeyedResult = null;
        } else {
            page(page.first());
        }
    }

    public void lastPage() {
        checkPagination();
        if (keyedPage != null) {
            throw new UnsupportedOperationException(
                    "Cannot navigate to last page in cursor-based pagination");
        }
        page(page.index(pageCount() - 1));
    }

    public boolean hasNextPage() {
        checkPagination();
        if (keyedPage != null) {
            if (lastKeyedResult == null) {
                throw new UnsupportedOperationException(
                        "Cannot call hasNextPage() before fetching results with list()");
            }
            return !lastKeyedResult.isLastPage();
        }
        return page.index < (pageCount() - 1);
    }

    public boolean hasPreviousPage() {
        checkPagination();
        if (keyedPage != null) {
            if (lastKeyedResult == null) {
                throw new UnsupportedOperationException(
                        "Cannot call hasPreviousPage() before fetching results with list()");
            }
            return !lastKeyedResult.isFirstPage();
        }
        return page.index > 0;
    }

    public int pageCount() {
        checkPagination();
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        int pageSize = keyedPage != null ? keyedPage.getPage().getSize() : page.size;
        return (int) Math.ceil((double) count / (double) pageSize);
    }

    public Page page() {
        checkPagination();
        return page;
    }

    private void checkPagination() {
        if (page == null && keyedPage == null) {
            throw new UnsupportedOperationException("Cannot call a page related method, " +
                    "call page(Page) or page(int, int) to initiate pagination first");
        }
        if (range != null) {
            throw new UnsupportedOperationException("Cannot call a page related method in a ranged query, " +
                    "call page(Page) or page(int, int) to initiate pagination first");
        }
    }

    private void checkRange() {
        if (range == null) {
            throw new UnsupportedOperationException("Cannot call a range related method, " +
                    "call range(int, int) to initiate range first");
        }
        if (page != null) {
            throw new UnsupportedOperationException("Cannot call a range related method in a paged query, " +
                    "call range(int, int) to initiate range first");
        }
    }

    public Range range() {
        checkRange();
        return range;
    }

    public void range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        this.page = null;
        this.keyedPage = null;
        this.lastKeyedResult = null;
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
        if (keyedPage != null) {
            SelectionQuery hibernateQuery = createBaseQuery();
            try (NonThrowingCloseable c = applyFilters()) {
                lastKeyedResult = hibernateQuery.getKeyedResultList((KeyedPage) keyedPage);
                return (List<T>) lastKeyedResult.getResultList();
            }
        }
        SelectionQuery hibernateQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            return hibernateQuery.getResultList();
        }
    }

    public <T extends Entity> Stream<T> stream() {
        if (keyedPage != null) {
            SelectionQuery hibernateQuery = createBaseQuery();
            try (NonThrowingCloseable c = applyFilters()) {
                lastKeyedResult = hibernateQuery.getKeyedResultList((KeyedPage) keyedPage);
                // there's no support for getKeyedResultStream() yet in ORM
                return (Stream<T>) lastKeyedResult.getResultList().stream();
            }
        }
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
                String orderBy = PanacheJpaUtil.toOrderBy(sort);
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
