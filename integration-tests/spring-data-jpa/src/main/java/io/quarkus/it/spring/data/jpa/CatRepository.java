package io.quarkus.it.spring.data.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Demonstrates various derived queries
 */
public interface CatRepository extends CrudRepository<Cat, Long> {

    Cat findCatByBreed(String breed);

    Optional<Cat> findByColor(String color);

    Stream<Cat> findCatByColorAndBreedAllIgnoreCase(String color, String breed);

    List<Cat> findCatsByColorIgnoreCaseAndBreed(String color, String breed);

    List<Cat> findByColorIsNotNullOrderByIdDesc();

    List<Cat> findByColorOrBreed(String color, String breed);

    List<Cat> findByBreedContaining(String breed);

    List<Cat> findByColorStartingWithOrBreedEndingWith(String color, String breed);

    Long countByColorIgnoreCase(String color);

    Long countByColorContainsIgnoreCase(String color);

    Boolean existsByColorStartingWith(String color);

    long removeCatsByColor(String color);

    List<Cat> findCatsByBreedIsIn(Collection<String> breeds);

    List<Cat> findByDistinctiveFalse();

    // issue 9192
    @Query(value = "SELECT c.distinctive FROM Cat c where c.id = :id")
    boolean customFindDistinctive(@Param("id") Long id);

    // the DISTINCT statement was left out intentionally to verify that duplicates are eliminated by the Set
    @Query(value = "SELECT c.color FROM Cat c WHERE c.color IS NOT NULL")
    Set<String> customQueryCatColors();
}
