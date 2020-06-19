package io.quarkus.it.spring.data.jpa;

import java.time.ZonedDateTime;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/post")
public class PostResource {

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;

    public PostResource(PostRepository postRepository, PostCommentRepository postCommentRepository) {
        this.postRepository = postRepository;
        this.postCommentRepository = postCommentRepository;
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

    @GET
    @Produces("application/json")
    @Path("/organization/{org}")
    public List<Post> findAllByOrganization(@PathParam("org") String org) {
        return postRepository.findAllByOrganization(org);
    }

    @GET
    @Produces("application/json")
    @Path("/postComment/postId/{id}")
    public List<PostComment> findByPostId(@PathParam("id") Long id) {
        return postCommentRepository.findByPostId(id);
    }
}
