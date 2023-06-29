package io.quarkus.it.spring.data.jpa;

import java.util.List;

/**
 * Used to ensure that entity relationships work correctly
 */
public interface PostRepository extends IntermediatePostRepository<Object, Post, Object> {

    List<Post> findAllByOrganization(String organization);

    long deleteByOrganization(String organization);
}
