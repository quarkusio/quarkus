package io.quarkus.it.spring.data.jpa.complex;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ParentBaseRepository<T> extends JpaRepository<T, Long> {
    Optional<SmallParent> findSomethingByIdAndName(Long id, String name);

    interface SmallParent {
        int getAge();

        float getTest();
    }

}
