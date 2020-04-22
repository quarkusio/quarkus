package io.quarkus.it.spring.data.jpa;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public String all() {
        Pageable wholePage = Pageable.unpaged();
        Page<Song> page = songRepository.findAll(wholePage);
        return page.hasPrevious() + " - " + page.hasNext() + " / " + page.getNumberOfElements();
    }

    @GET
    @Produces("application/json")
    @Path("/page/{num}/{size}")
    public String songs(@PathParam("num") int pageNum, @PathParam("size") int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum, pageSize);
        Page<Song> page = songRepository.findAll(pageRequest);
        return page.hasPrevious() + " - " + page.hasNext() + " / " + page.getNumberOfElements();
    }
}
