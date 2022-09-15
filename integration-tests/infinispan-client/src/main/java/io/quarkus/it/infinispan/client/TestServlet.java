package io.quarkus.it.infinispan.client;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import io.quarkus.infinispan.client.Remote;

@Path("/test")
public class TestServlet {
    private static final Log log = LogFactory.getLog(TestServlet.class);

    @Inject
    CacheSetup cacheSetup;

    @Inject
    @Remote(CacheSetup.DEFAULT_CACHE)
    RemoteCache<String, Book> cache;

    @Inject
    @Remote(CacheSetup.MAGAZINE_CACHE)
    RemoteCache<String, Magazine> magazineCache;

    @Inject
    @Remote(CacheSetup.AUTHORS_CACHE)
    RemoteCache<String, Author> authorsCache;

    @Inject
    CounterManager counterManager;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public List<String> getIDs() {
        cacheSetup.ensureStarted();
        log.info("Retrieving all IDs");
        return cache.keySet().stream().sorted().collect(Collectors.toList());
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCachedValue(@PathParam("id") String id) {
        cacheSetup.ensureStarted();
        Book book = cache.get(id);
        return book != null ? book.getTitle() : "NULL";
    }

    @Path("query/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String queryAuthorSurname(@PathParam("id") String name) {
        cacheSetup.ensureStarted();
        QueryFactory queryFactory = Search.getQueryFactory(cache);
        Query query = queryFactory.from(Book.class)
                .having("authors.name").like("%" + name + "%")
                .build();
        List<Book> list = query.execute().list();
        if (list.isEmpty()) {
            return "No one found for " + name;
        }

        return list.stream()
                .map(Book::getAuthors)
                .flatMap(Set::stream)
                .map(author -> author.getName() + " " + author.getSurname())
                .sorted()
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Path("icklequery/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ickleQueryAuthorSurname(@PathParam("id") String name) {
        cacheSetup.ensureStarted();
        QueryFactory queryFactory = Search.getQueryFactory(cache);
        Query query = queryFactory.create("from book_sample.Book b where b.authors.name like '%" + name + "%'");
        List<Book> list = query.execute().list();
        if (list.isEmpty()) {
            return "No one found for " + name;
        }
        return list.stream()
                .map(Book::getAuthors)
                .flatMap(Set::stream)
                .map(author -> author.getName() + " " + author.getSurname())
                .sorted()
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Path("incr/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<Long> incrementCounter(@PathParam("id") String id) {
        cacheSetup.ensureStarted();
        CounterConfiguration configuration = counterManager.getConfiguration(id);
        if (configuration == null) {
            configuration = CounterConfiguration.builder(CounterType.BOUNDED_STRONG).build();
            counterManager.defineCounter(id, configuration);
        }
        StrongCounter strongCounter = counterManager.getStrongCounter(id);
        return strongCounter.incrementAndGet();
    }

    @Path("cq")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String continuousQuery() {
        cacheSetup.ensureStarted();
        return cacheSetup.getMatches().values().stream()
                .mapToInt(Book::getPublicationYear)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(","));
    }

    @Path("nearcache")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String nearCache() {
        cacheSetup.ensureStarted();
        RemoteCacheClientStatisticsMXBean stats = cache.clientStatistics();
        long nearCacheMisses = stats.getNearCacheMisses();
        long nearCacheHits = stats.getNearCacheHits();
        long nearCacheInvalidations = stats.getNearCacheInvalidations();

        Book nearCacheBook = new Book("Near Cache Book", "Just here to test", 2010,
                Collections.emptySet(), Type.PROGRAMMING, new BigDecimal("12.99"));

        String id = "nearcache";
        cache.put(id, nearCacheBook);

        Book retrievedBook = cache.get(id);
        if (retrievedBook == null) {
            return "Couldn't retrieve id on first attempt";
        }

        long misses = stats.getNearCacheMisses();
        if (nearCacheMisses + 1 != misses) {
            return "Near cache didn't miss for some reason. Expected: " + (nearCacheMisses + 1) + " but got: " + misses;
        }

        if (!retrievedBook.equals(nearCacheBook)) {
            return "first retrieved book doesn't match";
        }

        retrievedBook = cache.get(id);
        if (retrievedBook == null) {
            return "Couldn't retrieve id on second attempt";
        }

        long hits = stats.getNearCacheHits();

        if (nearCacheHits + 1 != hits) {
            return "Near cache didn't hit for some reason. Expected: " + (nearCacheHits + 1) + " but got: " + hits;
        }

        if (!retrievedBook.equals(nearCacheBook)) {
            return "second retrieved book doesn't match";
        }

        nearCacheBook = new Book("Near Cache Book", "Just here to test",
                2011, Collections.emptySet(), Type.PROGRAMMING, new BigDecimal("0.99"));

        cache.put(id, nearCacheBook);

        long invalidations = stats.getNearCacheInvalidations();
        if (nearCacheInvalidations + 1 != invalidations) {
            // Try a second time after waiting just a little bit
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            invalidations = stats.getNearCacheInvalidations();
            if (nearCacheInvalidations + 1 != invalidations) {
                return "Near cache didn't invalidate for some reason. Expected: " + (nearCacheInvalidations + 1) + " but got: "
                        + invalidations;
            }
        }

        cache.remove(id);

        return "worked";
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createItem(String value, @PathParam("id") String id) {
        cacheSetup.ensureStarted();
        Book book = new Book(id, value, 2019, Collections.emptySet(), Type.PROGRAMMING, new BigDecimal("9.99"));
        Book previous = cache.putIfAbsent(id, book);
        if (previous == null) {
            //status code 201
            return Response.status(Response.Status.CREATED)
                    .entity(id)
                    .build();
        } else {
            return Response.noContent()
                    .build();
        }
    }

    @Path("magazinequery/{id}")
    @GET
    public String magazineQuery(@PathParam("id") String name) {
        cacheSetup.ensureStarted();
        QueryFactory queryFactory = Search.getQueryFactory(magazineCache);
        Query query = queryFactory.create("from magazine_sample.Magazine m where m.name like '%" + name + "%'");
        List<Magazine> list = query.execute().list();
        if (list.isEmpty()) {
            return "No one found for " + name;
        }
        return list.stream()
                .map(m -> m.getName() + ":" + m.getPublicationYearMonth())
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Path("create-cache-default-config/authors")
    @GET
    public String magazineQuery() {
        cacheSetup.ensureStarted();
        return authorsCache.values().stream()
                .map(a -> a.getName())
                .collect(Collectors.joining(",", "[", "]"));
    }
}
