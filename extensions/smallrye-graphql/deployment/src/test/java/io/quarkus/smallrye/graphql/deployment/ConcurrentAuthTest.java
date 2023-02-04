package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.getPropertyAsString;
import static io.restassured.RestAssured.given;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

public class ConcurrentAuthTest extends AbstractGraphQLTest {

    static Map<String, String> PROPERTIES = new HashMap<>();
    static {

        PROPERTIES.put("quarkus.smallrye-graphql.error-extension-fields", "classification,code");
        PROPERTIES.put("quarkus.smallrye-graphql.show-runtime-exception-message", "java.lang.SecurityException");

        PROPERTIES.put("quarkus.http.auth.basic", "true");
        PROPERTIES.put("quarkus.security.users.embedded.enabled", "true");
        PROPERTIES.put("quarkus.security.users.embedded.plain-text", "true");
        PROPERTIES.put("quarkus.security.users.embedded.users.scott", "jb0ss");
    }

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FilmResource.class, Film.class, GalaxyService.class)
                    .addAsResource(new StringAsset(getPropertyAsString(PROPERTIES)), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    private int iterations = 5000;

    @Test
    public void concurrentAllFilmsOnly() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(50);

        var futures = new ArrayList<CompletableFuture<Boolean>>(iterations);
        for (int i = 0; i < iterations; i++) {
            futures.add(CompletableFuture.supplyAsync(this::allFilmsRequestWithAuth, executor)
                    .thenApply(r -> !r.getBody().asString().contains("unauthorized")));
        }
        Optional<Boolean> success = getTestResult(futures);
        Assertions.assertTrue(success.orElse(false), "Unauthorized response codes were found");
        executor.shutdown();
    }

    private static Optional<Boolean> getTestResult(ArrayList<CompletableFuture<Boolean>> futures)
            throws InterruptedException, ExecutionException {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .reduce(Boolean::logicalAnd))
                .get();
    }

    private Response allFilmsRequestWithAuth() {
        String requestBody = "{\"query\":" +
                "\"" +
                "{" +
                " allFilmsSecured  {" +
                " title" +
                " director" +
                " releaseDate" +
                " episodeID" +
                "}" +
                "}" +
                "\"" +
                "}";

        return given()
                .body(requestBody)
                .auth()
                .preemptive()
                .basic("scott", "jb0ss")
                .post("/graphql/");
    }

    @GraphQLApi
    public static class FilmResource {

        @Inject
        GalaxyService service;

        @Query("allFilmsSecured")
        @Authenticated
        public List<Film> getAllFilmsSecured() {
            return service.getAllFilms();
        }
    }

    public static class Film {

        private String title;
        private Integer episodeID;
        private String director;
        private LocalDate releaseDate;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getEpisodeID() {
            return episodeID;
        }

        public void setEpisodeID(Integer episodeID) {
            this.episodeID = episodeID;
        }

        public String getDirector() {
            return director;
        }

        public void setDirector(String director) {
            this.director = director;
        }

        public LocalDate getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(LocalDate releaseDate) {
            this.releaseDate = releaseDate;
        }

    }

    @ApplicationScoped
    public static class GalaxyService {

        private List<Film> films = new ArrayList<>();

        public GalaxyService() {

            Film aNewHope = new Film();
            aNewHope.setTitle("A New Hope");
            aNewHope.setReleaseDate(LocalDate.of(1977, Month.MAY, 25));
            aNewHope.setEpisodeID(4);
            aNewHope.setDirector("George Lucas");

            Film theEmpireStrikesBack = new Film();
            theEmpireStrikesBack.setTitle("The Empire Strikes Back");
            theEmpireStrikesBack.setReleaseDate(LocalDate.of(1980, Month.MAY, 21));
            theEmpireStrikesBack.setEpisodeID(5);
            theEmpireStrikesBack.setDirector("George Lucas");

            Film returnOfTheJedi = new Film();
            returnOfTheJedi.setTitle("Return Of The Jedi");
            returnOfTheJedi.setReleaseDate(LocalDate.of(1983, Month.MAY, 25));
            returnOfTheJedi.setEpisodeID(6);
            returnOfTheJedi.setDirector("George Lucas");

            films.add(aNewHope);
            films.add(theEmpireStrikesBack);
            films.add(returnOfTheJedi);
        }

        public List<Film> getAllFilms() {
            return films;
        }
    }
}
