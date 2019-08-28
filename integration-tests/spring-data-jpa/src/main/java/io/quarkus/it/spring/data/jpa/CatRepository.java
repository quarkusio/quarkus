package io.quarkus.it.spring.data.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.repository.CrudRepository;

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

    Boolean existsByColorStartingWith(String color);

    long removeCatsByColor(String color);

    List<Cat> findCatsByBreedIsIn(Collection<String> breeds);

    List<Cat> findByDistinctiveFalse();
}
