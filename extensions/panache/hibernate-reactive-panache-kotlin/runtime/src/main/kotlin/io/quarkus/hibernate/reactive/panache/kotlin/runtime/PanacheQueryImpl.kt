package io.quarkus.hibernate.reactive.panache.kotlin.runtime;

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheQuery
import io.quarkus.hibernate.reactive.panache.common.runtime.CommonPanacheQueryImpl;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.persistence.LockModeType;
import java.util.Collections;

class PanacheQueryImpl<Entity> : PanacheQuery<Entity> {

    private var delegate: CommonPanacheQueryImpl<Entity>

    constructor(em: Uni<Mutiny.Session>, query: String, orderBy: String, paramsArrayOrMap: Any?) {
        this.delegate = CommonPanacheQueryImpl<Entity>(em, query, orderBy, paramsArrayOrMap);
    }

    private constructor(delegate: CommonPanacheQueryImpl<Entity>) {
        this.delegate = delegate
    }

    // Builder
    override fun <NewEntity : Any> project(type: Class<NewEntity>): PanacheQuery<NewEntity> {
        return PanacheQueryImpl(delegate.project(type))
    }

    override fun page(page: Page?): PanacheQuery<Entity> {
        delegate.page(page);
        return this
    }

    override fun page(pageIndex: Int, pageSize: Int): PanacheQuery<Entity> {
        delegate.page(pageIndex, pageSize);
        return this
    }

    override fun nextPage(): PanacheQuery<Entity> {
        delegate.nextPage();
        return this
    }

    override fun previousPage(): PanacheQuery<Entity> {
        delegate.previousPage();
        return this
    }

    override fun firstPage(): PanacheQuery<Entity> {
        delegate.firstPage();
        return this
    }

    override fun lastPage(): Uni<PanacheQuery<Entity>> = delegate.lastPage().map { _ -> this }


    override fun hasNextPage(): Uni<Boolean> =
        delegate.hasNextPage()

    override fun hasPreviousPage(): Boolean =
        delegate.hasPreviousPage()

    override fun pageCount(): Uni<Int> =
        delegate.pageCount()

    override fun page(): Page =
        delegate.page()

    override fun range(startIndex: Int, lastIndex: Int): PanacheQuery<Entity> {
        delegate.range(startIndex, lastIndex);
        return this
    }

    override fun withLock(lockModeType: LockModeType): PanacheQuery<Entity> {
        delegate.withLock(lockModeType);
        return this
    }

    override fun withHint(hintName: String, value: Any): PanacheQuery<Entity> {
        delegate.withHint(hintName, value);
        return this
    }

    override fun filter(filterName: String, parameters: Parameters): PanacheQuery<Entity> {
        delegate.filter(filterName, parameters.map());
        return this
    }

    override fun filter(filterName: String, parameters: Map<String, Any>): PanacheQuery<Entity> {
        delegate.filter(filterName, parameters);
        return this
    }

    override fun filter(filterName: String): PanacheQuery<Entity> {
        delegate.filter(filterName, Collections.emptyMap());
        return this
    }

    // Results
    override fun count(): Uni<Long> =
        delegate.count()


    override fun list(): Uni<List<Entity>> =
        delegate.list();

    override fun stream(): Multi<Entity> =
        delegate.stream()

    override fun firstResult(): Uni<Entity> =
        delegate.firstResult()


    override fun singleResult(): Uni<Entity> =
        delegate.singleResult()

}
