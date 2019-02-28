package io.quarkus.camel.runtime;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.camel.spi.UuidGenerator;

public class FastUuidGenerator implements UuidGenerator {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    final char[] seed = (longToHex(new char[0], ThreadLocalRandom.current().nextLong()).substring(1) + "-").toCharArray();
    final AtomicLong index = new AtomicLong();

    @Override
    public String generateUuid() {
        return longToHex(seed, index.getAndIncrement());
    }

    private static String longToHex(char[] seed, long v) {
        int l = seed.length;
        char[] hexChars = new char[16 + seed.length];
        System.arraycopy(seed, 0, hexChars, 0, l);
        for (int j = 15; j >= 0; j--) {
            hexChars[l + j] = HEX_ARRAY[(int) (v & 0x0F)];
            v >>= 4;
        }
        return new String(hexChars);
    }

}
