package io.quarkus.it.resteasy.jackson;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("art")
public class Article {

    private String title;
    private String description;
    private String body;
    private List<String> tagList;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getTagList() {
        return tagList;
    }

    public void setTagList(List<String> tagList) {
        this.tagList = tagList;
    }
}
