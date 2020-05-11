package io.quarkus.it.spring.data.jpa;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface PostCommentRepository extends Repository<PostComment, Long> {

    List<PostComment> findByPostId(Long postId);
}
