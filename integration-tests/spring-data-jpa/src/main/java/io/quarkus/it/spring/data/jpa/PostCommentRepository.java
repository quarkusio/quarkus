package io.quarkus.it.spring.data.jpa;

import java.util.List;

import org.springframework.data.repository.RepositoryDefinition;

@RepositoryDefinition(domainClass = PostComment.class, idClass = Long.class)
public interface PostCommentRepository {

    List<PostComment> findAll();

    List<PostComment> findByPostId(Long postId);
}
