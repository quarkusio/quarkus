package io.quarkus.hibernate.panache.reactive;

import java.util.List;

import io.quarkus.hibernate.orm.panache.common.ProjectedFieldName;
import io.quarkus.hibernate.panache.PanacheQuery;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.smallrye.mutiny.Uni;

public interface PanacheReactiveQuery<Entity> extends PanacheQuery<Uni<Entity>, Uni<List<Entity>>, Uni<Boolean>, Uni<Long>> {

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
    public <T> PanacheQuery<Uni<T>, Uni<List<T>>, Boolean, Long> project(Class<T> type);
}
