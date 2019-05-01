package io.quarkus.hibernate.search.elasticsearch.test;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * This particular entity is not a valid entity as it is not marked with @Entity.
 * <p>
 * It is done this way just for the sake of testing the extension bootstrap.
 */
@Indexed
public class IndexedEntity {

}
