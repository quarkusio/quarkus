package io.quarkus.it.spring.data.jpa;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Path("/song")
public class SongResource {

    private final SongRepository songRepository;

    private final PersonRepository personRepository;

    public SongResource(SongRepository songRepository, PersonRepository personRepository) {
        this.songRepository = songRepository;
        this.personRepository = personRepository;
    }

    @Path("/new/{title}/author/{author}")
    @GET
    @Produces("application/json")
    public Song newSong(@PathParam("title") String title, @PathParam("author") String author) {
        Song song = new Song();
        song.setTitle(title);
        song.setAuthor(author);
        return songRepository.save(song);
    }

    @GET
    @Produces("application/json")
    @Path("/allPages")
    public String allPages() {
        Pageable wholePage = Pageable.unpaged();
        Page<Song> page = songRepository.findAll(wholePage);
        return page.hasPrevious() + " - " + page.hasNext() + " / " + page.getNumberOfElements();
    }

    @GET
    @Produces("application/json")
    @Path("/all")
    public Iterable<Song> all() {
        return songRepository.findAll();
    }

    @GET
    @Produces("application/json")
    @Path("/page/{num}/{size}")
    public String songs(@PathParam("num") int pageNum, @PathParam("size") int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum, pageSize);
        Page<Song> page = songRepository.findAll(pageRequest);
        return page.hasPrevious() + " - " + page.hasNext() + " / " + page.getNumberOfElements();
    }

    @Path("/id/{id}")
    @GET
    @Produces("application/json")
    public Optional<Song> findById(@PathParam("id") Long id) {
        return songRepository.findById(id);
    }

    @Path("/doNothing")
    @GET
    @Produces("application/json")
    public void doNothing() {
        songRepository.doNothing();
    }

    @Transactional
    @Path("/delete/all")
    @GET
    public void deleteAll() {
        Iterable<Song> all = songRepository.findAll();
        for (Song song : all) {
            List<Person> personByLikedSong = personRepository.findPersonByLikedSong(song.getId());
            personByLikedSong.forEach(person -> song.removePerson(person));
        }
        songRepository.deleteAll();
    }

    @Transactional
    @Path("/delete/id/{id}")
    @GET
    public void deleteById(@PathParam("id") Long id) {
        Optional<Song> songById = songRepository.findById(id);
        List<Person> personByLikedSong = personRepository.findPersonByLikedSong(id);
        if (songById.isPresent()) {
            Song song = songById.get();
            personByLikedSong.forEach(person -> song.removePerson(person));
            songRepository.delete(song);
        }

    }

    @Transactional
    @Path("/delete/{title}/author/{author}")
    @GET
    public void deleteByNameAndAuthor(@PathParam("title") String title, @PathParam("author") String author) {
        Optional<Song> songById = songRepository.findSongByTitleAndAuthor(title, author);
        if (songById.isPresent()) {
            List<Person> personByLikedSong = personRepository.findPersonByLikedSong(songById.get().getId());
            Song song = songById.get();
            personByLikedSong.forEach(person -> song.removePerson(person));
            songRepository.delete(song);
        }

    }
}
