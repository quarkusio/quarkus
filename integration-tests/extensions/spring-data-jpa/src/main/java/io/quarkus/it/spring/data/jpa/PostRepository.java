package io.quarkus.it.spring.data.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Used to ensure that entity relationships work correctly
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    Post findFirstByBypassTrue();
}
