package io.quarkus.it.spring.data.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Demonstrates the use of methods from JpaRepository
 */
public interface CountryRepository extends JpaRepository<Country, Long> {
}
