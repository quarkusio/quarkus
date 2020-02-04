package io.quarkus.qute.deployment;

import java.util.Arrays;
import java.util.List;

public class Movie {

    public List<String> mainCharacters;

    public Movie(String... mainCharacters) {
        this.mainCharacters = Arrays.asList(mainCharacters);
    }

}