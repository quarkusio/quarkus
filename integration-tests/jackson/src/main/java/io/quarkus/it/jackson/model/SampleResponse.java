package io.quarkus.it.jackson.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@RegisterForReflection
public class SampleResponse {

    private String blogTitle;

    public SampleResponse() {
    }

    public SampleResponse(String blogTitle) {
        this.blogTitle = blogTitle;
    }

    public String getBlogTitle() {
        return blogTitle;
    }

    public void setBlogTitle(String blogTitle) {
        this.blogTitle = blogTitle;
    }

    @Override
    public String toString() {
        return "SampleResponse{" +
                "blogTitle='" + blogTitle + '\'' +
                '}';
    }
}