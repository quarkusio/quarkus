package io.quarkus.it.spring.data.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface SongRepository extends PagingAndSortingRepository<Song, Long> {

}
