package io.quarkus.spring.data.deployment.multiple_pu.first;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FirstEntityRepository extends CrudRepository<FirstEntity, Long> {
}
