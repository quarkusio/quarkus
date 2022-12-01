package io.quarkus.spring.data.rest.paged;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface DefaultRecordsRepository extends PagingAndSortingRepository<Record, Long> {
}
