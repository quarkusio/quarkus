package io.quarkus.it.spring.data.jpa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

@Path("/movie")
public class MovieResource {

    private final MovieRepository movieRepository;

    public MovieResource(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @GET
    @Path("/all")
    @Produces("application/json")
    public Iterable<Movie> all() {
        return movieRepository.findAll();
    }

    @GET
    @Path("/first/orderByDuration")
    @Produces("application/json")
    public Movie findFirstByOrOrderByTitleDesc() {
        return movieRepository.findFirstByOrderByDurationDesc();
    }

    @GET
    @Path("/title/{title}")
    @Produces("application/json")
    public Movie findByTitle(@PathParam("title") String title) {
        return movieRepository.findByTitle(title);
    }

    @GET
    @Path("/title/titleLengthOrder/page/{size}/{num}")
    public String orderByTitleLengthSlice(@PathParam("size") int pageSize, @PathParam("num") int pageNum) {
        Slice<Movie> slice = movieRepository.findByDurationGreaterThan(1,
                PageRequest.of(pageNum, pageSize, Sort.Direction.ASC, "title"));
        return slice.hasNext() + " / " + slice.getNumberOfElements();
    }

    @GET
    @Path("/customFind/page/{size}/{num}")
    public String customFind(@PathParam("size") int pageSize, @PathParam("num") int pageNum) {
        Page<Movie> page = movieRepository.customFind(
                PageRequest.of(pageNum, pageSize, Sort.Direction.ASC, "title"));
        return page.hasNext() + " / " + page.getNumberOfElements();
    }

    @GET
    @Path("/customFind/all")
    public List<Movie> customFindReturnAll() {
        Page<Movie> page = movieRepository.customFind(
                PageRequest.of(0, 100, Sort.Direction.DESC, "id"));
        return page.stream().collect(Collectors.toList());
    }

    @GET
    @Path("/rating/{rating}")
    @Produces("application/json")
    public List<Movie> findByRating(@PathParam("rating") String rating) {
        Iterator<Movie> byRating = movieRepository.findByRating(rating);
        List<Movie> result = new ArrayList<>();
        byRating.forEachRemaining(result::add);
        return result;
    }

    @GET
    @Path("/rating/{rating}/durationLargerThan/{duration}")
    @Produces("application/json")
    public List<Movie> withTitleAndDurationLargerThan(@PathParam("rating") String rating, @PathParam("duration") int duration) {
        return movieRepository.withRatingAndDurationLargerThan(duration, rating);
    }

    @GET
    @Path("/title/like/{title}")
    @Produces("application/json")
    public List<Object[]> someFieldsWithTitleLike(@PathParam("title") String title) {
        return movieRepository.someFieldsWithTitleLike(title, Sort.by(new Sort.Order(Sort.Direction.ASC, "duration")));
    }

    @GET
    @Path("/delete/rating/{rating}")
    public void deleteByRating(@PathParam("rating") String rating) {
        movieRepository.deleteByRating(rating);
    }

    @GET
    @Path("/delete/title/{title}")
    public Long deleteByTitleLike(@PathParam("title") String title) {
        return movieRepository.deleteByTitleLike(title);
    }

    @GET
    @Path("/change/rating/{rating}/{newRating}")
    public Integer changeRatingToNewName(@PathParam("rating") String rating, @PathParam("newRating") String newRating) {
        return movieRepository.changeRatingToNewName(newRating, rating);
    }

    @GET
    @Path("/nullify/rating/forTitle/{title}")
    public void setRatingToNullForTitle(@PathParam("title") String title) {
        movieRepository.setRatingToNullForTitle(title);
    }

    @GET
    @Path("/count/rating")
    @Produces("application/json")
    public List<MovieRepository.MovieCountByRating> countByRating() {
        List<MovieRepository.MovieCountByRating> list = movieRepository.countByRating();
        // #6205 - Make sure elements in list have been properly cast to the target object type.
        // If the type is wrong (Object array), this will throw a ClassNotFoundException
        MovieRepository.MovieCountByRating first = list.get(0);
        Objects.requireNonNull(first);

        return list;
    }

    @GET
    @Path("/rating/forTitle/{title}")
    @Produces("application/json")
    public MovieRepository.MovieRating titleRating(@PathParam("title") String title) {
        MovieRepository.MovieRating result = movieRepository.findRatingByTitle(title);
        Objects.requireNonNull(result);
        return result;
    }

    @GET
    @Path("/rating/opt/forTitle/{title}")
    @Produces("application/json")
    public Optional<MovieRepository.MovieRating> optionalTitleRating(@PathParam("title") String title) {
        Optional result = movieRepository.findOptionalRatingByTitle(title);
        return result;
    }

    @GET
    @Path("/ratings")
    @Produces("application/json")
    public List<String> findAllRatings() {
        return movieRepository.findAllRatings();
    }

    @Path("/new/{id}/{title}")
    @GET
    @Produces("application/json")
    public Movie newPerson(@PathParam("id") Long id, @PathParam("title") String title) {
        Movie movie = new Movie(id, title, null, -1);
        movie = movieRepository.save(movie);

        movie.setDuration(1000);
        return movieRepository.save(movie);
    }

    @GET
    @Path("/titles/rating/{rating}")
    @Produces("application/json")
    public List<MovieRepository.MovieProjection> getTitlesByRating(@PathParam("rating") String rating) {
        return movieRepository.findTitleAndRatingByRating(rating);
    }

}
