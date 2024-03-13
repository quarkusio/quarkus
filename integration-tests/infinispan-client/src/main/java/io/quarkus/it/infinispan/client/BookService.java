package io.quarkus.it.infinispan.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.infinispan.client.Remote;

@ApplicationScoped
public class BookService {
    public static final String DEFAULT_DESCRIPTION = "Default description";

    @Inject
    RemoteCacheManager cacheManager;

    @Inject
    @Remote(CacheSetup.BOOKS_CACHE)
    RemoteCache<String, Book> books;

    public String getBookDescriptionById(String id) {
        Book book = books.get(id);
        if (book == null) {
            return DEFAULT_DESCRIPTION;
        }

        return book.description();
    }

    public String getBookDescriptionById(String cacheName, String id) {
        RemoteCache<String, Book> cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalArgumentException("Cache not found");
        }
        Book book = cache.get(id);
        if (book == null) {
            return DEFAULT_DESCRIPTION;
        }
        return book.description();
    }
}
