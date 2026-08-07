package io.quarkus.hibernate.orm.runtime.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;

import org.hibernate.cache.spi.entry.CacheEntry;
import org.junit.jupiter.api.Test;

class DehydratedEntityWeigherTest {

    @Test
    void weighsStringPayloadsByLength() {
        CacheEntry entry = new StubCacheEntry(new Serializable[] { "abcdefghij" });
        int weight = DehydratedEntityWeigher.INSTANCE.weigh("key", entry);
        assertThat(weight).isGreaterThan(DehydratedEntityWeigher.INSTANCE.weigh("key",
                new StubCacheEntry(new Serializable[] { "ab" })));
    }

    @Test
    void weighsByteArraysByLength() {
        CacheEntry large = new StubCacheEntry(new Serializable[] { new byte[1000] });
        CacheEntry small = new StubCacheEntry(new Serializable[] { new byte[10] });
        assertThat(DehydratedEntityWeigher.INSTANCE.weigh("k", large))
                .isGreaterThan(DehydratedEntityWeigher.INSTANCE.weigh("k", small));
    }

    @Test
    void alwaysReturnsAtLeastOne() {
        assertThat(DehydratedEntityWeigher.INSTANCE.weigh(null, null)).isGreaterThanOrEqualTo(1);
    }

    private static final class StubCacheEntry implements CacheEntry {
        private final Serializable[] state;

        private StubCacheEntry(Serializable[] state) {
            this.state = state;
        }

        @Override
        public boolean isReferenceEntry() {
            return false;
        }

        @Override
        public String getSubclass() {
            return "stub";
        }

        @Override
        public Object getVersion() {
            return null;
        }

        @Override
        public Serializable[] getDisassembledState() {
            return state;
        }
    }
}
