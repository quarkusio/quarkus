package io.quarkus.it.spring.data.jpa;

import java.time.ZonedDateTime;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/post")
public class PostResource {

    private final PostRepository postRepository;

    public PostResource(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GET
    @Produces("application/json")
    @Path("/all")
    public List<Post> all() {
        return postRepository.findAll();
    }

    @GET
    @Produces("application/json")
    @Path("/bypass/true")
    public Post byBypassTrue() {
        return postRepository.findFirstByBypassTrue();
    }

    @GET
    @Produces("application/json")
    @Path("/postedBeforeNow")
    public List<Post> findByPostedAtAfter() {
        return postRepository.findByPostedBefore(ZonedDateTime.now());
    }
}
