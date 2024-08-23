package io.quarkus.it.jackson.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SampleResponse {

    private String blogTitle;
    private String name;

    public SampleResponse() {
    }

    public SampleResponse(String blogTitle, String name) {
        this.blogTitle = blogTitle;
        this.name = name;
    }

    public String getBlogTitle() {
        return blogTitle;
    }

    public void setBlogTitle(String blogTitle) {
        this.blogTitle = blogTitle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "SampleResponse{" +
                "blogTitle='" + blogTitle + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
