package io.quarkus.spring.data.rest.paged;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DefaultRecordsRepository extends JpaRepository<Record, Long> {
}
