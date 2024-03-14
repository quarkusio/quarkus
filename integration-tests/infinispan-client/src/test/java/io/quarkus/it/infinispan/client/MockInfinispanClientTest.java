package io.quarkus.it.infinispan.client;

import static io.quarkus.it.infinispan.client.BookService.DEFAULT_DESCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.junit.jupiter.api.Test;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MockInfinispanClientTest {

    @Inject
    BookService bookService;

    @InjectMock
    RemoteCacheManager cacheManager;

    @InjectMock
    @Remote(CacheSetup.BOOKS_CACHE)
    RemoteCache<String, Book> bookRemoteCache;

    private final Book BOOK = new Book("Full Saga of Harry Potter", "Best saga ever", 1997,
            Collections.emptySet(), Type.FANTASY, new BigDecimal("500.99"));

    @Test
    public void mockRemoteCacheManager() {
        RemoteCache<String, Book> localMockCache = mock(RemoteCache.class);
        when(localMockCache.get("harry_potter")).thenReturn(BOOK);
        when(cacheManager.<String, Book> getCache(CacheSetup.BOOKS_CACHE)).thenReturn(localMockCache);

        assertThat(bookService.getBookDescriptionById(CacheSetup.BOOKS_CACHE, "non_exist")).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(bookService.getBookDescriptionById(CacheSetup.BOOKS_CACHE, "harry_potter")).isEqualTo("Best saga ever");
        assertThatIllegalArgumentException().isThrownBy(
                () -> bookService.getBookDescriptionById(CacheSetup.DEFAULT_CACHE, "non_exist"));
    }

    @Test
    public void mockRemoteCache() {
        when(bookRemoteCache.get("harry_potter")).thenReturn(BOOK);
        assertThat(bookService.getBookDescriptionById("non_exist")).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(bookService.getBookDescriptionById("harry_potter")).isEqualTo("Best saga ever");
    }
}
