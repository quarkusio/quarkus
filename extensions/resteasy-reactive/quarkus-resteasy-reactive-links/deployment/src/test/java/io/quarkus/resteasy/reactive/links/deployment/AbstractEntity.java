package io.quarkus.resteasy.reactive.links.deployment;

public abstract class AbstractEntity {

    private int id;

    private String slug;

    public AbstractEntity() {
    }

    protected AbstractEntity(int id, String slug) {
        this.id = id;
        this.slug = slug;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
