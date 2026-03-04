package io.quarkus.spring.data.deployment;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface CategoryRepository extends Repository<Category, Long> {

    List<Category> findByCategory(String category);

    List<Category> findByName(String name);
}
