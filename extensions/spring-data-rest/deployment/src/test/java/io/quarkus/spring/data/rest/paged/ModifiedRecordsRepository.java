package io.quarkus.spring.data.rest.paged;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(exported = false, path = "secret", collectionResourceRel = "secret-records")
public interface ModifiedRecordsRepository extends JpaRepository<Record, Long> {

    @Override
    @RestResource(path = "records")
    Page<Record> findAll(Pageable pageable);

    @Override
    @RestResource(path = "records")
    Optional<Record> findById(Long id);
}
