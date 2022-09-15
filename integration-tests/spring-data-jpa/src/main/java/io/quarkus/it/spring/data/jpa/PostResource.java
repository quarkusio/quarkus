package io.quarkus.it.spring.data.jpa;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/post")
public class PostResource {

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;

    public PostResource(PostRepository postRepository, PostCommentRepository postCommentRepository) {
        this.postRepository = postRepository;
        this.postCommentRepository = postCommentRepository;
    }

    @Path("/new/title/{title}/organization/{organization}")
    @GET
    @Produces("application/json")
    public Post newPost(@PathParam("title") String title, @PathParam("organization") String organization) {
        Post post = new Post();
        post.setTitle(title);
        post.setOrganization(organization);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("someLabel1", "someValue1");
        metadata.put("someLabel2", "someValue2");
        post.setMetadata(metadata);
        PostComment postComment = new PostComment("new comment for post from " + organization);
        postComment.setPost(post);
        post.getComments().add(postComment);
        return postRepository.save(post);
    }

    @POST
    @Produces("application/json")
    @Path("/postId/{id}/key/{key}/value/{value}")
    public Optional<Post> addMetadata(@PathParam("id") Long id, @PathParam("key") String key, @PathParam("key") String value) {
        Optional<Post> optionalPost = postRepository.findById(id);
        Map<String, String> metadata = new HashMap<>();
        metadata.put(key, value);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            post.setMetadata(metadata);
            return Optional.of(postRepository.save(post));
        }
        return Optional.empty();
    }

    @POST
    @Produces("application/json")
    @Path("/postComment/postId/{id}/comment/{comment}")
    public Optional<Post> addComment(@PathParam("id") Long id, @PathParam("comment") String comment) {
        PostComment postComment = new PostComment(comment);
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            post.addComment(postComment);
            return Optional.of(postRepository.save(post));
        }
        return Optional.empty();
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

    @GET
    @Produces("application/json")
    @Path("/mandatory/{id}")
    public Post findMandatoryByPostId(@PathParam("id") Long id) {
        return postRepository.findMandatoryById(id);
    }

    @GET
    @Produces("application/json")
    @Path("/doNothing")
    public void doNothing() {
        postRepository.doNothing();
    }

    @GET
    @Produces("application/json")
    @Path("/postComment/all")
    public List<PostComment> findAll() {
        return postCommentRepository.findAll();
    }

    @Path("/delete/all")
    @GET
    public void deleteAll() {
        postRepository.deleteAll();
    }

    @Path("/delete/byOrg/{org}")
    @GET
    public void deleteByOrganization(@PathParam("org") String organization) {
        postRepository.deleteByOrganization(organization);
    }

}
