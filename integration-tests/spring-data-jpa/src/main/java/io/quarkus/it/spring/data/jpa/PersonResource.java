package io.quarkus.it.spring.data.jpa;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Path("/person")
public class PersonResource {

    @Inject
    PersonRepository personRepository;

    @Inject
    SongRepository songRepository;

    @Path("/new/{name}")
    @GET
    @Produces("application/json")
    public Person newPerson(@PathParam("name") String name) {
        Person person = new Person(name);
        personRepository.makeNameUpperCase(person);
        personRepository.doNothing();
        personRepository.getName(person);
        return personRepository.save(person);
    }

    @Path("/new/{name}/times/{times}")
    @GET
    @Produces("application/json")
    public Iterable<Person> newPeople(@PathParam("name") String name, @PathParam("times") Integer times) {
        List<Person> people = new ArrayList<>(times);
        IntStream.rangeClosed(1, times).forEach(i -> {
            people.add(new Person(name));
        });
        return personRepository.saveAll(people);
    }

    @Path("/id/{id}")
    @GET
    @Produces("application/json")
    public Optional<Person> findById(@PathParam("id") Long id) {
        return personRepository.findById(id);
    }

    @Path("/exists/id/{id}")
    @GET
    @Produces("application/json")
    public boolean existsById(@PathParam("id") Long id) {
        return personRepository.existsById(id);
    }

    @GET
    @Path("/all")
    @Produces("application/json")
    public Iterable<Person> all() {
        personRepository.doNothingMore();
        return personRepository.findAll();
    }

    @Path("/ids/{ids}")
    @GET
    @Produces("application/json")
    public Iterable<Person> findById(@PathParam("ids") String ids) {
        return personRepository.findAllById(Stream.of(ids.split(",")).map(Long::valueOf).collect(Collectors.toList()));
    }

    @Path("/count")
    @GET
    public long count() {
        return personRepository.count();
    }

    @Path("/delete/{id}")
    @GET
    public void delete(@PathParam("id") Long id) {
        personRepository.deleteById(id);
    }

    @Transactional
    @Path("/delete/name/first/{name}")
    @GET
    public void deleteFirstByName(@PathParam("name") String name) {
        List<Person> byName = personRepository.findByName(name);
        if (byName.isEmpty()) {
            throw new IllegalArgumentException("no Person found with name = " + name);
        }
        personRepository.delete(byName.get(0));
    }

    @Transactional
    @Path("/delete/name/all/{name}")
    @GET
    public void deleteAllByName(@PathParam("name") String name) {
        personRepository.deleteAll(personRepository.findByName(name));
    }

    @Transactional
    @Path("/delete/age/{age}")
    @GET
    public void deleteByAge(@PathParam("age") Integer age) {
        personRepository.deleteByAge(age);
    }

    @Path("/delete/all")
    @GET
    public void deleteAll() {
        personRepository.deleteAll();
    }

    @GET
    @Path("/name/{name}")
    @Produces("application/json")
    public List<Person> byName(@PathParam("name") String name) {
        return personRepository.findByName(name);
    }

    @GET
    @Path("/name-pageable/{name}")
    @Produces("text/plain")
    public String byNamePageable(@PathParam("name") String name) {
        return personRepository.findByName(name, PageRequest.of(0, 2, Sort.by(new Sort.Order(Sort.Direction.DESC, "id"))))
                .stream().map(Person::getId).map(Object::toString).collect(Collectors.joining(","));
    }

    @GET
    @Path("/name/joinedOrder/{name}/page/{size}/{num}")
    public String byNamePage(@PathParam("name") String name, @PathParam("size") int pageSize, @PathParam("num") int pageNum) {
        Page<Person> page = personRepository.findByNameOrderByJoined(name, PageRequest.of(pageNum, pageSize));
        return page.hasPrevious() + " - " + page.hasNext() + " / " + page.getNumberOfElements();
    }

    @GET
    @Path("/name/{name}/order/{field}")
    @Produces("application/json")
    public List<Person> byNameOrderByField(@PathParam("name") String name, @PathParam("field") String field) {
        return personRepository.findByName(name,
                Sort.by(new Sort.Order(Sort.Direction.DESC, field), new Sort.Order(Sort.Direction.DESC, "id")));
    }

    @GET
    @Path("/name/ageOrder/{name}")
    @Produces("application/json")
    public List<Person> byNameOrder(@PathParam("name") String name) {
        return personRepository.findByNameOrderByAge(name);
    }

    @GET
    @Path("/name/ageOrder/{name}/page/{size}/{num}")
    @Produces("application/json")
    public List<Person> byNameOrder(@PathParam("name") String name, @PathParam("size") int pageSize,
            @PathParam("num") int pageNum) {
        return personRepository.findByNameOrderByAgeDesc(name, PageRequest.of(pageNum, pageSize));
    }

    @GET
    @Path("/age/between/{lower}/{upper}")
    @Produces("application/json")
    public List<Person> byAgeBetween(@PathParam("lower") int lowerBound, @PathParam("upper") int upperBound) {
        return personRepository.findByAgeBetweenAndNameIsNotNull(lowerBound, upperBound);
    }

    @GET
    @Path("/age/greaterEqual/{lower}")
    @Produces("application/json")
    public List<Person> byAgeGreaterEqual(@PathParam("lower") int lowerBound) {
        return personRepository.findByAgeGreaterThanEqualOrderByAgeAsc(lowerBound);
    }

    @GET
    @Path("/joined/afterDaysAgo/{daysAgo}")
    @Produces("application/json")
    public List<Person> joinedAfter(@PathParam("daysAgo") int daysAgo) {
        return personRepository.queryByJoinedIsAfter(changeNow(LocalDate.now(), LocalDate::minusDays, daysAgo));
    }

    @GET
    @Path("/active")
    @Produces("application/json")
    public Collection<Person> active() {
        return personRepository.readByActiveTrueOrderByAgeDesc();
    }

    @GET
    @Path("/count/activeNot/{value}")
    public Long activeCount(@PathParam("value") boolean value) {
        return personRepository.countByActiveNot(value);
    }

    @GET
    @Path("/active/top3")
    @Produces("application/json")
    public List<Person> findTop3ByActive() {
        return personRepository.findTop3ByActive(true, Sort.by(new Sort.Order(Sort.Direction.DESC, "age")));
    }

    @GET
    @Path("/addressZipCode/{zipCode}")
    @Produces("application/json")
    public List<Person> findPeopleByAddressZipCode(@PathParam("zipCode") String zipCode) {
        return personRepository.findPeopleBySomeAddressZipCode(zipCode);
    }

    @GET
    @Path("/addressId/{id}")
    @Produces("application/json")
    public List<Person> findByAddressId(@PathParam("id") Long id) {
        return personRepository.findBySomeAddressId(id);
    }

    @GET
    @Path("/addressStreetNumber/{streetNumber}")
    @Produces("application/json")
    public List<Person> findByAddressStreetNumber(@PathParam("streetNumber") String streetNumber) {
        return personRepository.findBySomeAddressStreetNumber(streetNumber);
    }

    @GET
    @Path("/{id}/song/{idSong}")
    @Produces("application/json")
    public Person addLikedSongToPerson(@PathParam("id") Long id, @PathParam("idSong") Long idSong) {
        Optional<Person> person = personRepository.findById(id);
        Optional<Song> song = songRepository.findById(idSong);
        if (person.isPresent() && song.isPresent()) {
            person.get().getLikedSongs().add(song.get());
            return personRepository.save(person.get());
        }
        return null;
    }

    @GET
    @Path("/{id}/songs")
    @Produces("application/json")
    public List<Song> findPersonLikedSongs(@PathParam("id") Long id) {
        return songRepository.findPersonLikedSongs(id);
    }

    private Date changeNow(LocalDate now, BiFunction<LocalDate, Long, LocalDate> function, long diff) {
        return Date.from(function.apply(now, diff).atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }
}
