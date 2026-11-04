package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrimitiveCreatorPojo {

    private final String title;
    private final int pages;

    public PrimitiveCreatorPojo() {
        this(null, 0);
    }

    @JsonCreator
    public PrimitiveCreatorPojo(@JsonProperty("title") String title, @JsonProperty("pages") int pages) {
        this.title = title;
        this.pages = pages;
    }

    public String getTitle() {
        return title;
    }

    public int getPages() {
        return pages;
    }
}
