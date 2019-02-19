package io.quarkus.example.test;

import java.util.concurrent.ThreadLocalRandom;

public class Utils {

    public static String randomString() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; ++i) {
            sb.append((char) random.nextInt('A', 'Z' + 1));
        }
        return sb.toString();
    }

}
