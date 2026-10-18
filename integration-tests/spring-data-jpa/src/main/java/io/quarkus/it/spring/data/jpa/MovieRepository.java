package io.quarkus.it.spring.data.jpa;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import io.quarkus.it.spring.data.jpa.test.MovieRating;

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
    List<Movie> someFieldsWithTitleLike(String title, Sort sort);

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

    // issue 13044
    @Query("SELECT DISTINCT m.rating FROM Movie m where m.rating is not null")
    List<String> findAllRatings();

    @Query("SELECT title, rating from Movie where title = ?1")
    Optional<MovieRating> findOptionalRatingByTitle(String title);

    @Query("SELECT title, rating FROM Movie WHERE title = ?1")
    MovieRating findRatingByTitle(String title);

    // issue 13050
    List<MovieProjection> findTitleAndRatingByRating(String rating);

    interface MovieCountByRating {
        String getRating();

        Long getCount();
    }

    interface MovieProjection {
        String getTitle();

        String getRating();
    }

}
