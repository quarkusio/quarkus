package io.quarkus.it.spring.data.jpa;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.data.domain.Pageable;

@Path("/song")
public class SongResource {

    private final SongRepository songRepository;

    public SongResource(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    @GET
    @Produces("application/json")
    @Path("/all")
    public List<Song> all() {
        Pageable wholePage = Pageable.unpaged();
        return songRepository.findAll();
    }
}
