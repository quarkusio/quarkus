/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.example.infinispanclient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import io.quarkus.infinispan.client.runtime.Remote;
import io.quarkus.runtime.StartupEvent;

@Path("/")
@ApplicationScoped
public class TestServlet {
    private static final Log log = LogFactory.getLog(TestServlet.class);

    @Inject
    @Remote("default")
    RemoteCache<String, Book> cache;

    @Inject
    CounterManager counterManager;

    CountDownLatch waitUntilStarted = new CountDownLatch(1);

    final Map<String, Book> matches = new ConcurrentHashMap<>();

    void onStart(@Observes StartupEvent ev) {
        log.info("Servlet has started");

        cache.addClientListener(new EventPrintListener());

        log.info("Added client listener");

        ContinuousQuery<String, Book> continuousQuery = Search.getContinuousQuery(cache);

        QueryFactory queryFactory = Search.getQueryFactory(cache);
        Query query = queryFactory.from(Book.class)
                .having("publicationYear").gt(2011)
                .build();

        ContinuousQueryListener<String, Book> listener = new ContinuousQueryListener<String, Book>() {
            @Override
            public void resultJoining(String key, Book value) {
                log.warn("Adding key: " + key + " for book: " + value);
                matches.put(key, value);
            }

            @Override
            public void resultLeaving(String key) {
                log.warn("Removing key: " + key);
                matches.remove(key);
            }
        };

        continuousQuery.addContinuousQueryListener(query, listener);

        log.info("Added continuous query listener");

        cache.put("book1", new Book("Game of Thrones", "Lots of people perish", 2010,
                Collections.singleton(new Author("George", "Martin"))));
        cache.put("book2", new Book("Game of Thrones Path 2", "They win?", 2023,
                Collections.singleton(new Author("Son", "Martin"))));

        log.info("Inserted values");

        waitUntilStarted.countDown();
    }

    @ClientListener
    static class EventPrintListener {

        @ClientCacheEntryCreated
        public void handleCreatedEvent(ClientCacheEntryCreatedEvent e) {
            log.warn("Someone has created an entry: " + e);
        }

        @ClientCacheEntryModified
        public void handleModifiedEvent(ClientCacheEntryModifiedEvent e) {
            log.warn("Someone has modified an entry: " + e);
        }

        @ClientCacheEntryRemoved
        public void handleRemovedEvent(ClientCacheEntryRemovedEvent e) {
            log.warn("Someone has removed an entry: " + e);
        }

    }

    /**
     * This is needed because start notification is done async - you can receive requests while running start
     */
    private void ensureStart() {
        try {
            if (!waitUntilStarted.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException(new TimeoutException());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public List<String> getIDs() {
        ensureStart();
        log.fatal("Retrieving all IDs");
        return cache.keySet().stream().sorted().collect(Collectors.toList());
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCachedValue(@PathParam("id") String id) {
        ensureStart();
        Book book = cache.get(id);
        return book.getTitle();
    }

    @Path("query/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String queryAuthorSurname(@PathParam("id") String name) {
        ensureStart();
        QueryFactory queryFactory = Search.getQueryFactory(cache);
        Query query = queryFactory.from(Book.class)
                .having("authors.name").like("%" + name + "%")
                .build();
        List<Book> list = query.list();
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
        ensureStart();
        QueryFactory queryFactory = Search.getQueryFactory(cache);
        Query query = queryFactory.create("from book_sample.Book b where b.authors.name like '%" + name + "%'");
        List<Book> list = query.list();
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
        ensureStart();
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
        ensureStart();
        return matches.values().stream()
                .mapToInt(Book::getPublicationYear)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(","));
    }

    @Path("nearcache")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String nearCache() {
        ensureStart();
        RemoteCacheClientStatisticsMXBean stats = cache.clientStatistics();
        long nearCacheMisses = stats.getNearCacheMisses();
        long nearCacheHits = stats.getNearCacheHits();
        long nearCacheInvalidations = stats.getNearCacheInvalidations();

        Book nearCacheBook = new Book("Near Cache Book", "Just here to test", 2010,
                Collections.emptySet());

        String id = "nearcache";
        cache.put(id, nearCacheBook);

        Book retrievedBook = cache.get(id);
        if (retrievedBook == null) {
            return "Couldn't retrieve id on first attempt";
        }

        long misses = stats.getNearCacheMisses();
        if (nearCacheMisses + 1 != misses) {
            return "Near cache didn't miss for some reason. Expected: " + nearCacheMisses + 1 + " but got: " + misses;
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
            return "Near cache didn't hit for some reason. Expected: " + nearCacheHits + 1 + " but got: " + hits;
        }

        if (!retrievedBook.equals(nearCacheBook)) {
            return "second retrieved book doesn't match";
        }

        nearCacheBook = new Book("Near Cache Book", "Just here to test", 2011, Collections.emptySet());

        cache.put(id, nearCacheBook);

        long invalidations = stats.getNearCacheInvalidations();
        if (nearCacheInvalidations + 1 != invalidations) {
            return "Near cache didn't invalidate for some reason. Expected: " + nearCacheInvalidations + 1 + " but got: "
                    + invalidations;
        }

        cache.remove(id);

        return "worked";
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createItem(String value, @PathParam("id") String id) {
        ensureStart();
        Book book = new Book(id, value, 2019, Collections.emptySet());
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
}
