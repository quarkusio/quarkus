package io.quarkus.it.spring.data.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface SongRepository extends ListCrudRepository<Song, Long>, ListPagingAndSortingRepository<Song, Long> {

    @Query(value = "SELECT s FROM Song s JOIN s.likes l WHERE l.id = :personId")
    List<Song> findPersonLikedSongs(@Param("personId") Long personId);

    Optional<Song> findSongByTitleAndAuthor(String title, String author);

    default void doNothing() {

    }

}
