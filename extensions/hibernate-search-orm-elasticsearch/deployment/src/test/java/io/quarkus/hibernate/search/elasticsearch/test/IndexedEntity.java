package io.quarkus.hibernate.search.elasticsearch.test;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * An indexed entity.
 * <p>
 * Technically this particular class is not a valid entity as it is not marked with @Entity.
 * However, that's not relevant for our tests, and it's easier this way.
 */
@Indexed
public class IndexedEntity {

}
