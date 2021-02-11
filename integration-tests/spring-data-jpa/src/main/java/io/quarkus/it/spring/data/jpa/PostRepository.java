package io.quarkus.it.spring.data.jpa;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Used to ensure that entity relationships work correctly
 */
public interface PostRepository extends IntermediateRepository<Post, Long> {

    Post findFirstByBypassTrue();

    List<Post> findByPostedBefore(ZonedDateTime zdt);

    List<Post> findAllByOrganization(String organization);

    long deleteByOrganization(String organization);
}
