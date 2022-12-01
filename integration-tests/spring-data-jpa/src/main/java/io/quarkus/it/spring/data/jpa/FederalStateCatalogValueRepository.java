package io.quarkus.it.spring.data.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FederalStateCatalogValueRepository extends JpaRepository<FederalStateCatalogValue, Long> {

    // note: key is defined in superclass of FederalStateCatalogValue
    FederalStateCatalogValue findByKey(String key);
}
