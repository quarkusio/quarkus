package io.quarkus.it.spring.data.rest;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleJpaRepository extends JpaRepository<Article, Long> {
}
