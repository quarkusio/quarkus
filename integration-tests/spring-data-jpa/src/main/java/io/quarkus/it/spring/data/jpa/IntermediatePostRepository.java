package io.quarkus.it.spring.data.jpa;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.repository.NoRepositoryBean;

/**
 * Used to ensure that entity relationships work correctly
 */
@NoRepositoryBean
public interface IntermediatePostRepository<T extends ByPassHolder> extends BypassHolderRepository<T, Long> {

    List<T> findByPostedBefore(ZonedDateTime zdt);
}
