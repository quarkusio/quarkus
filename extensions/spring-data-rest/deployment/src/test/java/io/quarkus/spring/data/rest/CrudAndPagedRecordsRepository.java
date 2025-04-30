package io.quarkus.spring.data.rest;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import io.quarkus.spring.data.rest.paged.Record;

public interface CrudAndPagedRecordsRepository extends PagingAndSortingRepository<Record, Long>, CrudRepository<Record, Long> {
}
