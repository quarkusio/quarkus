package io.quarkus.it.infinispan.client;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class CacheSetup {

    private static final Log log = LogFactory.getLog(CacheSetup.class);

    public static final String DEFAULT_CACHE = "default";
    public static final String MAGAZINE_CACHE = "magazine";
    public static final String BOOKS_CACHE = "books";
    public static final String AUTHORS_CACHE = "authors";

    @Inject
    RemoteCacheManager cacheManager;

    @Inject
    @Remote(AUTHORS_CACHE)
    RemoteCache<String, Author> authors;

    private final Map<String, Book> matches = new ConcurrentHashMap<>();

    private CountDownLatch waitUntilStarted = new CountDownLatch(1);

    void onStart(@Observes StartupEvent ev) {
        RemoteCache<String, Book> defaultCache = cacheManager.getCache(DEFAULT_CACHE);
        RemoteCache<String, Magazine> magazineCache = cacheManager.getCache(MAGAZINE_CACHE);
        RemoteCache<String, Book> booksCache = cacheManager.getCache(BOOKS_CACHE);

        defaultCache.addClientListener(new EventPrintListener());

        ContinuousQuery<String, Book> continuousQuery = Search.getContinuousQuery(defaultCache);

        QueryFactory queryFactory = Search.getQueryFactory(defaultCache);
        Query query = queryFactory.create("from book_sample.Book where publicationYear > 2011");

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

            @Override
            public void resultUpdated(String key, Book value) {
                log.warn("Entry updated: " + key);
            }
        };

        continuousQuery.addContinuousQueryListener(query, listener);

        log.info("Added continuous query listener");

        Author gMartin = new Author("George", "Martin");

        defaultCache.put("book1", new Book("Game of Thrones", "Lots of people perish", 2010,
                Collections.singleton(gMartin), Type.FANTASY, new BigDecimal("23.99")));
        defaultCache.put("book2", new Book("Game of Thrones Path 2", "They win?", 2023,
                Collections.singleton(new Author("Son", "Martin")), Type.FANTASY, new BigDecimal("54.99")));

        magazineCache.put("first-mad", new Magazine("MAD", YearMonth.of(1952, 10),
                Collections.singletonList("Blob named Melvin")));
        magazineCache.put("first-time", new Magazine("TIME", YearMonth.of(1923, 3),
                Arrays.asList("First helicopter", "Change in divorce law", "Adam's Rib movie released",
                        "German Reparation Payments")));
        magazineCache.put("popular-time", new Magazine("TIME", YearMonth.of(1997, 4),
                Arrays.asList("Yep, I'm gay", "Backlash against HMOS", "False Hope on Breast Cancer?")));

        authors.put("aut-1", gMartin);

        booksCache.put("hp-1", new Book("Philosopher's Stone", "Harry Potter and the Philosopher's Stone", 1997,
                Collections.singleton(new Author("J. K. Rowling", "Rowling")), Type.FANTASY, new BigDecimal("50.99")));
        booksCache.put("hp-2", new Book("Chamber of Secrets", "Harry Potter and the Chamber of Secrets", 1998,
                Collections.singleton(new Author("J. K. Rowling", "Rowling")), Type.FANTASY, new BigDecimal("50.99")));
        booksCache.put("hp-3", new Book("Prisoner of Azkaban", "Harry Potter and the Prisoner of Azkaban", 1999,
                Collections.singleton(new Author("J. K. Rowling", "Rowling")), Type.FANTASY, new BigDecimal("50.99")));

        waitUntilStarted.countDown();
    }

    public void ensureStarted() {
        try {
            if (!waitUntilStarted.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException(new TimeoutException());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Book> getMatches() {
        return matches;
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
}
