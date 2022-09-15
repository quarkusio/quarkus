package io.quarkus.it.spring.data.jpa;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "Post")
@Table(name = "post")
public class Post {

    @Id
    @SequenceGenerator(name = "postSeqGen", sequenceName = "postSeq", initialValue = 100, allocationSize = 1)
    @GeneratedValue(generator = "postSeqGen")
    private Long id;

    private String title;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PostComment> comments = new ArrayList<>();

    @ElementCollection
    private Map<String, String> metadata = new HashMap<>();

    private boolean bypass;

    private ZonedDateTime posted;

    private String organization;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<PostComment> getComments() {
        return comments;
    }

    public void setComments(List<PostComment> comments) {
        this.comments = comments;
    }

    public boolean isBypass() {
        return bypass;
    }

    public void setBypass(boolean bypass) {
        this.bypass = bypass;
    }

    public ZonedDateTime getPosted() {
        return posted;
    }

    public void setPosted(ZonedDateTime postedAt) {
        this.posted = postedAt;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void addComment(PostComment comment) {
        comments.add(comment);
        comment.setPost(this);
    }
}
