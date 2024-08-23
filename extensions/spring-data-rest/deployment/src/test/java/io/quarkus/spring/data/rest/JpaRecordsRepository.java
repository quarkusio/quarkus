package io.quarkus.spring.data.rest;

import org.springframework.data.jpa.repository.JpaRepository;

import io.quarkus.spring.data.rest.paged.Record;

public interface JpaRecordsRepository extends JpaRepository<Record, Long> {
}
