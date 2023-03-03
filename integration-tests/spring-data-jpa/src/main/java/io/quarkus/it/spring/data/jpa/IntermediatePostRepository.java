package io.quarkus.it.spring.data.jpa;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Used to ensure that entity relationships work correctly
 */
public interface IntermediatePostRepository extends BypassHolderRepository<Post, Long> {

    List<Post> findByPostedBefore(ZonedDateTime zdt);
}
