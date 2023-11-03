package io.quarkus.it.spring.data.jpa.complex;

import java.util.Optional;

import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean

public interface ParentMidRepository<T> extends ParentBaseRepository<T> {
    Optional<SmallParent2> findSmallParent2ByIdAndName(Long id, String name);

    interface SmallParent2 {
        int getAge();
    }
}
