package io.quarkus.it.infinispan.client;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class Magazine {
    private final String name;
    private final YearMonth publicationDate;
    private final List<String> stories;

    public Magazine(String name, YearMonth publicationDate, List<String> stories) {
        this.name = Objects.requireNonNull(name);
        this.publicationDate = Objects.requireNonNull(publicationDate);
        this.stories = Objects.requireNonNull(stories);
    }

    public String getName() {
        return name;
    }

    public YearMonth getPublicationYearMonth() {
        return publicationDate;
    }

    public List<String> getStories() {
        return stories;
    }
}
