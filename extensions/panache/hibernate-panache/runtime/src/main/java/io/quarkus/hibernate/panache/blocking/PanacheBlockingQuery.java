package io.quarkus.hibernate.panache.blocking;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.NonUniqueResultException;

import io.quarkus.hibernate.panache.PanacheQuery;

public interface PanacheBlockingQuery<Entity> extends PanacheQuery<Entity, List<Entity>, Boolean, Long> {

    /**
     * Defines a projection class. This will transform the returned values into instances of the given type using the following
     * mapping rules:
     * <ul>
     * <li>If your query already selects some specific columns (starts with <code>select distinct? a, b, c…</code>) then we
     * transform
     * it into a query of the form: <code>select distinct? new ProjectionClass(a, b, c)…</code>. There must be a matching
     * constructor
     * that accepts the selected column types, in the right order.</li>
     * <li>If your query does not select any specific column (starts with <code>from…</code>) then we transform it into a query
     * of the form:
     * <code>select new ProjectionClass(a, b, c…) from…</code> where we fetch the list of selected columns from your projection
     * class'
     * single constructor, using its parameter names (or their {@link ProjectedFieldName} annotations), in the same order as the
     * constructor.</li>
     * <li>If this is already a project query of the form <code>select distinct? new…</code>, we throw a
     * {@link PanacheQueryException}</li>
     *
     * @param type the projected class type
     * @return a new query with the same state as the previous one (params, page, range, lockMode, hints, ...) but a projected
     *         result of the type
     *         <code>type</code>
     * @throws PanacheQueryException if this represents an already-projected query
     */
    public <T> PanacheQuery<T, List<T>, Boolean, Long> project(Class<T> type);

    /**
     * Returns the current page of results as a {@link Stream}.
     *
     * @return the current page of results as a {@link Stream}.
     * @see #list()
     * @see #page(Page)
     * @see #page()
     */
    public Stream<Entity> stream();

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     *
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     * @see #singleResultOptional()
     */
    public Optional<Entity> firstResultOptional();

    /**
     * Executes this query for the current page and return a single result.
     *
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     * @throws NonUniqueResultException if there are more than one result
     * @see #firstResultOptional()
     */
    public Optional<Entity> singleResultOptional();
}
