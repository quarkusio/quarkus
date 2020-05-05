package io.quarkus.qute.deployment.typesafe;

import java.util.Arrays;
import java.util.List;

public class Movie {

    public final Boolean alwaysTrue;

    public final List<String> mainCharacters;

    public Movie(String... mainCharacters) {
        this.alwaysTrue = Boolean.TRUE;
        this.mainCharacters = Arrays.asList(mainCharacters);
    }

    public String getName() {
        return "Jason";
    }

    public Long findService(String name) {
        return 10l;
    }

    public Long findServices(String name, Long age) {
        return 11l;
    }

    public String findNames(long limit, String... names) {
        return "ok";
    }

}