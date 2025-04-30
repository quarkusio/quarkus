package io.quarkus.it.hibernate.search.standalone.elasticsearch.mapping;

public class EntityMappedProgrammatically {

    public static final String INDEX = "programmatic-index";

    private Long id;
    private String text;

    public EntityMappedProgrammatically(long id, String text) {
        this.id = id;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }
}
