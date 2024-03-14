package io.quarkus.it.infinispan.client;

import static io.quarkus.it.infinispan.client.CacheSetup.AUTHORS_CACHE;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.commons.api.query.Query;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.counter.api.WeakCounter;

import io.quarkus.infinispan.client.InfinispanClientName;
import io.quarkus.infinispan.client.Remote;
import io.smallrye.common.annotation.Blocking;

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
    @Remote(AUTHORS_CACHE)
    RemoteCache<String, Author> authorsCacheDefault;

    @Inject
    @InfinispanClientName("another")
    @Remote(AUTHORS_CACHE)
    RemoteCache<String, Author> authorsCacheAnother;

    @Inject
    CounterManager counterManager;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public List<String> getIDs() {
        log.info("Retrieving all IDs");
        return cache.keySet().stream().sorted().collect(Collectors.toList());
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCachedValue(@PathParam("id") String id) {
        Book book = cache.get(id);
        return book != null ? book.title() : "NULL";
    }

    @Path("query/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String queryAuthorSurname(@PathParam("id") String name) {
        Query<Book> query = cache.query("from book_sample.Book b where b.authors.name like '%" + name + "%'");
        List<Book> list = query.execute().list();
        if (list.isEmpty()) {
            return "No one found for " + name;
        }

        return list.stream()
                .map(Book::authors)
                .flatMap(Set::stream)
                .map(author -> author.name() + " " + author.surname())
                .sorted()
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Path("icklequery/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ickleQueryAuthorSurname(@PathParam("id") String name) {
        Query<Book> query = cache.query("from book_sample.Book b where b.authors.name like '%" + name + "%'");
        List<Book> list = query.execute().list();
        if (list.isEmpty()) {
            return "No one found for " + name;
        }
        return list.stream()
                .map(Book::authors)
                .flatMap(Set::stream)
                .map(author -> author.name() + " " + author.surname())
                .sorted()
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Path("counter/{id}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public boolean defineCounter(@PathParam("id") String id, @QueryParam("type") String type,
            @QueryParam("storage") String storage) {
        CounterConfiguration configuration = counterManager.getConfiguration(id);
        if (configuration == null) {
            configuration = CounterConfiguration.builder(CounterType.valueOf(type)).storage(Storage.valueOf(storage)).build();
            return counterManager.defineCounter(id, configuration);
        }
        return true;
    }

    @Path("incr/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public CompletionStage<Long> incrementCounter(@PathParam("id") String id) {
        CounterConfiguration configuration = counterManager.getConfiguration(id);
        if (configuration == null) {
            return CompletableFuture.completedFuture(0L);
        }

        if (configuration.type() == CounterType.WEAK) {
            WeakCounter weakCounter = counterManager.getWeakCounter(id);
            weakCounter.sync().increment();
            return CompletableFuture.completedFuture(weakCounter.getValue());
        }

        StrongCounter strongCounter = counterManager.getStrongCounter(id);
        return strongCounter.incrementAndGet();
    }

    @Path("cq")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String continuousQuery() {
        return cacheSetup.getMatches().values().stream()
                .mapToInt(Book::publicationYear)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(","));

    }

    @Path("nearcache")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String nearCache() {
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
        Query<Magazine> query = magazineCache.query(
                "from magazine_sample.Magazine m where m.name like '%" + name + "%'");
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
        List<String> names1 = authorsCacheDefault.values().stream().map(a -> a.name()).collect(Collectors.toList());
        List<String> names2 = authorsCacheAnother.values().stream().map(a -> a.name())
                .collect(Collectors.toList());

        names1.addAll(names2);
        return names1.stream().sorted().collect(Collectors.joining(",", "[", "]"));
    }
}
