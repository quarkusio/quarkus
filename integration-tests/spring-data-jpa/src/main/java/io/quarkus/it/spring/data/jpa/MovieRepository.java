package io.quarkus.it.spring.data.jpa;

import java.util.Iterator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Demonstrated the use @Query
 */
public interface MovieRepository extends CrudRepository<Movie, Long> {

    Movie findFirstByOrderByDurationDesc();

    @Query("select m from Movie m where m.rating = ?1")
    Iterator<Movie> findByRating(String rating);

    @Query("from Movie where title = ?1")
    Movie findByTitle(String title);

    @Query("select m from Movie m where m.duration > :duration and m.rating = :rating")
    List<Movie> withRatingAndDurationLargerThan(@Param("duration") int duration, @Param("rating") String rating);

    @Query("from Movie where title like concat('%', ?1, '%')")
    List<Object[]> someFieldsWithTitleLike(String title, Sort sort);

    @Modifying
    @Query("delete from Movie where rating = :rating")
    void deleteByRating(@Param("rating") String rating);

    @Modifying
    @Query("delete from Movie where title like concat('%', ?1, '%')")
    Long deleteByTitleLike(String title);

    @Modifying
    @Query("update Movie m set m.rating = :newName where m.rating = :oldName")
    int changeRatingToNewName(@Param("newName") String newName, @Param("oldName") String oldName);

    @Modifying
    @Query("update Movie set rating = null where title =?1")
    void setRatingToNullForTitle(String title);

    @Query("from Movie m where m.duration > ?1")
    Slice<Movie> findByDurationGreaterThan(Integer duration, Pageable pageable);

    @Query(value = "select m from Movie m", countQuery = "select count(m) from Movie m")
    Page<Movie> customFind(Pageable pageable);

    // issue 6205
    @Query(value = "SELECT rating, COUNT(*) FROM Movie GROUP BY rating")
    List<MovieCountByRating> countByRating();

    interface MovieCountByRating {
        String getRating();

        Long getCount();
    }
}
