package io.quarkus.it.spring.data.jpa;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;

public class DataGenerator {

    public static final Random RANDOM = new Random();

    public static Date randomDate() {
        return Date.from(LocalDate.now().minusDays(RANDOM.nextInt(3000)).atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    public static int randomAge() {
        return RANDOM.nextInt(100);
    }

    public static boolean randomBoolean() {
        return RANDOM.nextBoolean();
    }
}
