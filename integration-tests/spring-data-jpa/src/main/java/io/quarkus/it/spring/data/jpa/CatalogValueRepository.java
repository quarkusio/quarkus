package io.quarkus.it.spring.data.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogValueRepository extends JpaRepository<CatalogValue, Long> {

    CatalogValue findByKey(String key);
}
