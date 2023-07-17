package io.quarkus.it.spring.data.jpa;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.NoResultException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Path("/country")
public class CountryResource {

    private final CountryRepository countryRepository;

    public CountryResource(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @GET
    @Path("/all")
    @Produces("application/json")
    public List<Country> all() {
        return countryRepository.findAll(Sort.by(new Sort.Order(Sort.Direction.ASC, "iso3")));
    }

    @GET
    @Path("/page/{size}/{num}")
    public String page(@PathParam("size") int pageSize, @PathParam("num") int pageNum) {
        Page<Country> page = countryRepository.findAll(PageRequest.of(pageNum, pageSize));
        return page.hasPrevious() + " - " + page.hasNext() + " / " + page.getNumberOfElements();
    }

    @GET
    @Path("/page-sorted/{size}/{num}")
    @Produces("text/plain")
    public String pageSorted(@PathParam("size") int pageSize, @PathParam("num") int pageNum) {
        Page<Country> page = countryRepository.findAll(PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "id")));
        return page.stream().map(Country::getId).map(Object::toString).collect(Collectors.joining(","));
    }

    @GET
    @Path("/new/{name}/{iso3}")
    @Produces("application/json")
    public Country newCountry(@PathParam("name") String name, @PathParam("iso3") String iso3) {
        countryRepository.flush();
        return countryRepository.saveAndFlush(new Country(name, iso3));
    }

    @GET
    @Path("/editIso3/{id}/{iso3}")
    @Produces("application/json")
    public Country editIso3(@PathParam("id") Long id, @PathParam("iso3") String iso3) {
        Optional<Country> optional = countryRepository.findById(id);
        if (optional.isPresent()) {
            Country country = optional.get();
            country.setIso3(iso3);
            return countryRepository.save(country);
        } else {
            throw new NoResultException("No Country found with id =" + id);
        }
    }

    @GET
    @Path("/getOne/{id}")
    @Produces("application/json")
    public Country getOne(@PathParam("id") Long id) {
        return countryRepository.getOne(id);
    }

    @DELETE
    @Path("/")
    public void deleteAllInBatch() {
        this.countryRepository.deleteAllInBatch();
    }
}
