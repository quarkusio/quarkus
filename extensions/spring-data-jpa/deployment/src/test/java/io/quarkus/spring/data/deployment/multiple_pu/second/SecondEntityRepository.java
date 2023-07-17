package io.quarkus.spring.data.deployment.multiple_pu.second;

import org.springframework.stereotype.Repository;

@Repository
public interface SecondEntityRepository extends org.springframework.data.repository.Repository<SecondEntity, Long> {

    SecondEntity save(SecondEntity entity);

    long count();
}
