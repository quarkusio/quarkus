package io.quarkus.it.spring.data.jpa;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/cat")
public class CatResource {

    private final CatRepository catRepository;

    public CatResource(CatRepository catRepository) {
        this.catRepository = catRepository;
    }

    @GET
    @Produces("application/json")
    @Path("/all")
    public Iterable<Cat> all() {
        return catRepository.findAll();
    }

    @GET
    @Produces("application/json")
    @Path("/breed/{breed}")
    public Cat byBreed(@PathParam("breed") String breed) {
        return catRepository.findCatByBreed(breed);
    }

    @GET
    @Produces("application/json")
    @Path("/color/{color}")
    public Optional<Cat> optionalByColor(@PathParam("color") String color) {
        return catRepository.findByColor(color);
    }

    @GET
    @Produces("application/json")
    @Path("/by/color/{color}/breed/{breed}")
    public List<Cat> byColorAndBreed(@PathParam("color") String color, @PathParam("breed") String breed) {
        return catRepository.findCatByColorAndBreedAllIgnoreCase(color, breed).collect(Collectors.toList());
    }

    @GET
    @Produces("application/json")
    @Path("/by/color/startsWith/{color}/breed/endsWith/{breed}")
    public List<Cat> byColorStartingWithOrBreedEndingWith(@PathParam("color") String color, @PathParam("breed") String breed) {
        return catRepository.findByColorStartingWithOrBreedEndingWith(color, breed);
    }

    @GET
    @Produces("application/json")
    @Path("/by/breed/containing/{text}")
    public List<Cat> byBreedContaining(@PathParam("text") String breed) {
        return catRepository.findByBreedContaining(breed);
    }

    @GET
    @Produces("application/json")
    @Path("/by2/color/{color}/breed/{breed}")
    public List<Cat> byColorAndBreed2(@PathParam("color") String color, @PathParam("breed") String breed) {
        return catRepository.findCatsByColorIgnoreCaseAndBreed(color, breed);
    }

    @GET
    @Produces("application/json")
    @Path("/byOr/color/{color}/breed/{breed}")
    public List<Cat> byColorOrBreed(@PathParam("color") String color, @PathParam("breed") String breed) {
        return catRepository.findByColorOrBreed(color, breed);
    }

    @GET
    @Produces("application/json")
    @Path("/color/notNull")
    public List<Cat> colorNotNull() {
        return catRepository.findByColorIsNotNullOrderByIdDesc();
    }

    @GET
    @Path("/count/by/color/{color}")
    public Long countByColor(@PathParam("color") String color) {
        return catRepository.countByColorIgnoreCase(color);
    }

    @GET
    @Path("/count/by/color/{color}/contains")
    public Long countByColorContains(@PathParam("color") String color) {
        return catRepository.countByColorContainsIgnoreCase(color);
    }

    @GET
    @Path("/exists/by/colorStartsWith/{color}")
    public Boolean existsByColorStartsWith(@PathParam("color") String color) {
        return catRepository.existsByColorStartingWith(color);
    }

    @GET
    @Path("/delete/by/color/{color}")
    public void deleteByColor(@PathParam("color") String color) {
        catRepository.removeCatsByColor(color);
    }

    @Path("/findCatsByBreedIsIn")
    @GET
    @Produces("application/json")
    public List<Cat> byBreedInAndColorNotIn() {
        return catRepository.findCatsByBreedIsIn(Arrays.asList("dummy", "Persian", "British Shorthair"));
    }

    @GET
    @Produces("application/json")
    @Path("/by/distinctive/false")
    public List<Cat> byDistinctiveFalse() {
        return catRepository.findByDistinctiveFalse();
    }

    @GET
    @Path("/customFindDistinctive/{id}")
    @Produces("text/plain")
    public Boolean customFindDistinctive(@PathParam("id") Long id) {
        return catRepository.customFindDistinctive(id);
    }

    @GET
    @Path("/customQueryCatColors")
    @Produces("application/json")
    public Set<String> customQueryCatColors() {
        return catRepository.customQueryCatColors();
    }
}
