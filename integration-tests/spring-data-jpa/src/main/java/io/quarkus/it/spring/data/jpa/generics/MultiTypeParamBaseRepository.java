package io.quarkus.it.spring.data.jpa.generics;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface MultiTypeParamBaseRepository<T extends FatherBase<C>, C extends ChildBase> extends JpaRepository<T, Long> {
    List<T> findAllTestById(Long id);

    Optional<SmallParent> findSomethingByIdAndName(Long id, String name);

    GenericParent<C> findChildrenById(Long id);

    interface GenericParent<T extends ChildBase> {
        List<T> getChildren();
    }

    interface SmallParent {
        int getAge();

        float getTest();
    }
}
