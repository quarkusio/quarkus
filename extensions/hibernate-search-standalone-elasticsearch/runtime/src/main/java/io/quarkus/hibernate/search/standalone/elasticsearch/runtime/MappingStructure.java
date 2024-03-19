package io.quarkus.hibernate.search.standalone.elasticsearch.runtime;

public enum MappingStructure {

    // @formatter:off
    // @formatter:off
    /**
     * Entities indexed through Hibernate Search are nodes in an entity graph.
     *
     * With this structure:
     * * An indexed entity is independent of other entities it references through associations,
     * which *can* be updated independently of the indexed entity;
     * in particular they may be passed to
     * {@link org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan#addOrUpdate(Object)}.
     * * Therefore, when an entity changes,
     * Hibernate Search may need to resolve other entities to reindex,
     * which means in particular that associations between entities must be bi-directional:
     * specifying the inverse side of associations through `@AssociationInverseSide` *is required*,
     * unless reindexing is disabled for that association through `@IndexingDependency(reindexOnUpdate = ...)`.
     *
     * See also link:{hibernate-search-docs-url}#mapping-reindexing-associationinverseside[`@AssociationInverseSide`]
     * link:{hibernate-search-docs-url}#mapping-reindexing-reindexonupdate[`@IndexingDependency(reindexOnUpdate = ...)`].
     *
     * @asciidoclet
     */
    // @formatter:on
    GRAPH,

    /**
     * Entities indexed through Hibernate Search are the root of a document.
     *
     * With this structure:
     * * An indexed entity "owns" other entities it references through associations,
     * which *cannot* be updated independently of the indexed entity;
     * in particular they cannot be passed to
     * {@link org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan#addOrUpdate(Object)}.
     * * Therefore, when an entity changes,
     * Hibernate Search doesn't need to resolve other entities to reindex,
     * which means in particular that associations between entities can be uni-directional:
     * specifying the inverse side of associations through `@AssociationInverseSide` *is not required*.
     *
     * See also
     * link:{hibernate-search-docs-url}#mapping-reindexing-associationinverseside[`@AssociationInverseSide`].
     *
     * @asciidoclet
     */
    // @formatter:on
    DOCUMENT

}
