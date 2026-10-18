package io.quarkus.spring.data.deployment.multiple_pu.second;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SecondEntityRepository extends org.springframework.data.repository.Repository<SecondEntity, Long> {

    SecondEntity save(SecondEntity entity);

    long count();

    Optional<SecondEntity> findById(Long id);

    SecondEntity getOne(Long id);

    void deleteAll();

    void deleteByName(String name);

    Page<SecondEntity> findByName(String name, Pageable pageable);

    @Query(value = "SELECT se FROM SecondEntity se WHERE name=?1", countQuery = "SELECT COUNT(*) FROM SecondEntity se WHERE name=?1")
    Page<SecondEntity> findByNameQueryIndexed(String name, Pageable pageable);

    @Query(value = "SELECT se FROM SecondEntity se WHERE name=:name", countQuery = "SELECT COUNT(*) FROM SecondEntity se WHERE name=:name")
    Page<SecondEntity> findByNameQueryNamed(@Param("name") String name, Pageable pageable);

}
