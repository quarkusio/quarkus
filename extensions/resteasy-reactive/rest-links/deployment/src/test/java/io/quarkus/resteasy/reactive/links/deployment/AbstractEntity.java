package io.quarkus.resteasy.reactive.links.deployment;

public abstract class AbstractEntity extends AbstractId {

    private String slug;

    public AbstractEntity() {
    }

    protected AbstractEntity(int id, String slug) {
        super(id);
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
