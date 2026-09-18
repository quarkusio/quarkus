package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CacheResultPredicate;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Tests the {@code unless} attribute of {@link CacheResult}: a result the predicate rejects is returned but not kept in
 * the cache.
 */
public class CacheResultUnlessTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(CachedService.class, EmptyList.class, NegativeBean.class));

    @Inject
    CachedService cachedService;

    @BeforeEach
    public void reset() {
        cachedService.reset();
    }

    @Test
    public void rejectedResultIsComputedAgain() {
        assertEquals(List.of(), cachedService.list("nothing"));
        assertEquals(List.of(), cachedService.list("nothing"));
        assertEquals(2, cachedService.getListInvocations());
    }

    @Test
    public void acceptedResultIsCached() {
        assertEquals(List.of("something"), cachedService.list("something"));
        assertEquals(List.of("something"), cachedService.list("something"));
        assertEquals(1, cachedService.getListInvocations());
    }

    @Test
    public void beanPredicateIsUsedForAsyncResults() {
        assertEquals(-1, cachedService.number(-1).await().indefinitely());
        assertEquals(-1, cachedService.number(-1).await().indefinitely());
        assertEquals(2, cachedService.getNumberInvocations());

        assertEquals(5, cachedService.number(5).await().indefinitely());
        assertEquals(5, cachedService.number(5).await().indefinitely());
        assertEquals(3, cachedService.getNumberInvocations());
    }

    @ApplicationScoped
    static class CachedService {

        int listInvocations;
        int numberInvocations;

        public void reset() {
            listInvocations = 0;
            numberInvocations = 0;
        }

        public int getListInvocations() {
            return listInvocations;
        }

        public int getNumberInvocations() {
            return numberInvocations;
        }

        @CacheResult(cacheName = "unless-list", unless = EmptyList.class)
        public List<String> list(String key) {
            listInvocations++;
            return "nothing".equals(key) ? List.of() : List.of(key);
        }

        @CacheResult(cacheName = "unless-number", unless = NegativeBean.class)
        public Uni<Integer> number(int value) {
            return Uni.createFrom().item(() -> {
                numberInvocations++;
                return value;
            });
        }
    }

    public static class EmptyList implements CacheResultPredicate {

        @Override
        public boolean test(Object result) {
            return result instanceof List<?> list && list.isEmpty();
        }
    }

    @ApplicationScoped
    static class NegativeBean implements CacheResultPredicate {

        @Override
        public boolean test(Object result) {
            return result instanceof Integer i && i < 0;
        }
    }
}
