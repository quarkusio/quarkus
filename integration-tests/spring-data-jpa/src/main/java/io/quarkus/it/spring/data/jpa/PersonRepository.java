package io.quarkus.it.spring.data.jpa;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Demonstrates the ability to add fragments and some of the derived method capabilities
 */
public interface PersonRepository extends CrudRepository<Person, Long>, PersonFragment, PersonFragment2, PersonFragment3 {

    List<Person> findByName(String name);

    List<Person> findByName(String name, Pageable pageable);

    List<Person> findByName(String name, Sort sort);

    Page<Person> findByNameOrderByJoined(String name, Pageable pageable);

    List<Person> findByNameOrderByAge(String name);

    List<Person> findByNameOrderByAgeDesc(String name, PageRequest pageRequest);

    List<Person> findByAgeBetweenAndNameIsNotNull(int lowerAgeBound, int upperAgeBound);

    List<Person> findByAgeGreaterThanEqualOrderByAgeAsc(int age);

    List<Person> queryByJoinedIsAfter(Date date);

    Collection<Person> readByActiveTrueOrderByAgeDesc();

    Long countByActiveNot(boolean active);

    List<Person> findTop3ByActive(boolean active, Sort sort);

    List<Person> findPeopleBySomeAddressZipCode(String zipCode);

    List<Person> findBySomeAddressId(Long addressId);

    List<Person> findBySomeAddressStreetNumber(String streetName);

    long deleteByAge(Integer age);

    @Query(value = "SELECT p FROM Person p JOIN p.likedSongs s WHERE s.id = :songId")
    List<Person> findPersonByLikedSong(@Param("songId") Long songId);

}
