package io.quarkus.spring.data.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.jboss.jandex.DotName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryByExampleExecutor;

public final class DotNames {

    public static final DotName SPRING_DATA_REPOSITORY = DotName
            .createSimple(Repository.class.getName());
    public static final DotName SPRING_DATA_CRUD_REPOSITORY = DotName
            .createSimple(CrudRepository.class.getName());
    public static final DotName SPRING_DATA_PAGING_REPOSITORY = DotName
            .createSimple(PagingAndSortingRepository.class.getName());
    public static final DotName SPRING_DATA_JPA_REPOSITORY = DotName
            .createSimple(JpaRepository.class.getName());
    public static final DotName SPRING_DATA_QUERY_EXAMPLE_EXECUTOR = DotName
            .createSimple(QueryByExampleExecutor.class.getName());

    public static final Set<DotName> SUPPORTED_REPOSITORIES = new HashSet<>(Arrays.asList(
            SPRING_DATA_JPA_REPOSITORY, SPRING_DATA_PAGING_REPOSITORY, SPRING_DATA_CRUD_REPOSITORY, SPRING_DATA_REPOSITORY));

    public static final DotName SPRING_DATA_NO_REPOSITORY_BEAN = DotName
            .createSimple(NoRepositoryBean.class.getName());
    public static final DotName SPRING_DATA_PAGEABLE = DotName
            .createSimple(Pageable.class.getName());
    public static final DotName SPRING_DATA_PAGE_REQUEST = DotName
            .createSimple(PageRequest.class.getName());
    public static final DotName SPRING_DATA_SORT = DotName
            .createSimple(Sort.class.getName());
    public static final DotName SPRING_DATA_PAGE = DotName
            .createSimple(Page.class.getName());
    public static final DotName SPRING_DATA_SLICE = DotName
            .createSimple(Slice.class.getName());
    public static final DotName SPRING_DATA_QUERY = DotName
            .createSimple(Query.class.getName());
    public static final DotName SPRING_DATA_PARAM = DotName
            .createSimple(Param.class.getName());
    public static final DotName SPRING_DATA_MODIFYING = DotName
            .createSimple(Modifying.class.getName());

    public static final DotName JPA_ID = DotName.createSimple(Id.class.getName());
    public static final DotName JPA_MAPPED_SUPERCLASS = DotName.createSimple(MappedSuperclass.class.getName());
    public static final DotName VOID = DotName.createSimple(void.class.getName());
    public static final DotName LONG = DotName.createSimple(Long.class.getName());
    public static final DotName PRIMITIVE_LONG = DotName.createSimple(long.class.getName());
    public static final DotName INTEGER = DotName.createSimple(Integer.class.getName());
    public static final DotName PRIMITIVE_INTEGER = DotName.createSimple(int.class.getName());
    public static final DotName BOOLEAN = DotName.createSimple(Boolean.class.getName());
    public static final DotName PRIMITIVE_BOOLEAN = DotName.createSimple(boolean.class.getName());
    public static final DotName STRING = DotName.createSimple(String.class.getName());
    public static final DotName ITERATOR = DotName.createSimple(Iterator.class.getName());
    public static final DotName COLLECTION = DotName.createSimple(Collection.class.getName());
    public static final DotName LIST = DotName.createSimple(List.class.getName());
    public static final DotName STREAM = DotName.createSimple(Stream.class.getName());
    public static final DotName OPTIONAL = DotName.createSimple(Optional.class.getName());
    public static final DotName OBJECT = DotName.createSimple(Object.class.getName());

    private DotNames() {
    }
}
