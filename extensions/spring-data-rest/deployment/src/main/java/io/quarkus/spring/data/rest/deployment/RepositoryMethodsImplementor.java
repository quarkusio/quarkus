package io.quarkus.spring.data.rest.deployment;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.deployment.Constants;

public class RepositoryMethodsImplementor implements ResourceMethodsImplementor {

    //CrudRepository
    public static final MethodDesc GET = MethodDesc.of(CrudRepository.class, "findById", Optional.class, Object.class);
    public static final MethodDesc ADD = MethodDesc.of(CrudRepository.class, "save", Object.class, Object.class);
    public static final MethodDesc UPDATE = MethodDesc.of(CrudRepository.class, "save", Object.class, Object.class);

    public static final MethodDesc DELETE = MethodDesc.of(CrudRepository.class, "deleteById", void.class, Object.class);
    public static final MethodDesc LIST_ITERABLE = MethodDesc.of(CrudRepository.class, "findAll", Iterable.class);

    //ListCrudRepository
    public static final MethodDesc LIST = MethodDesc.of(ListCrudRepository.class, "findAll", List.class);
    public static final MethodDesc LIST_BY_ID = MethodDesc.of(ListCrudRepository.class, "findAllById", List.class,
            Iterable.class);
    public static final MethodDesc SAVE_LIST = MethodDesc.of(ListCrudRepository.class, "saveAll", List.class, Iterable.class);

    //PagingAndSortingRepository
    public static final MethodDesc LIST_PAGED = MethodDesc.of(PagingAndSortingRepository.class, "findAll",
            org.springframework.data.domain.Page.class, Pageable.class);

    private static final Class<?> PANACHE_PAGE = Page.class;

    private static final Class<?> PANACHE_SORT = Sort.class;

    private static final Class<?> PANACHE_COLUMN = Sort.Column.class;

    private static final Class<?> PANACHE_DIRECTION = Sort.Direction.class;

    public static final DotName CRUD_REPOSITORY_INTERFACE = DotName.createSimple(CrudRepository.class.getName());
    public static final DotName LIST_CRUD_REPOSITORY_INTERFACE = DotName.createSimple(ListCrudRepository.class.getName());

    public static final DotName PAGING_AND_SORTING_REPOSITORY_INTERFACE = DotName
            .createSimple(PagingAndSortingRepository.class.getName());

    public static final DotName LIST_PAGING_AND_SORTING_REPOSITORY_INTERFACE = DotName
            .createSimple(ListPagingAndSortingRepository.class.getName());

    public static final DotName JPA_REPOSITORY_INTERFACE = DotName.createSimple(JpaRepository.class.getName());

    protected final EntityClassHelper entityClassHelper;

    public RepositoryMethodsImplementor(IndexView index, EntityClassHelper entityClassHelper) {
        this.entityClassHelper = entityClassHelper;
    }

    //    CrudRepository Iterable<T> findAll();
    public void implementIterable(ClassCreator classCreator, String repositoryInterfaceName) {
        if (entityClassHelper.isCrudRepository(repositoryInterfaceName)
                && !entityClassHelper.isPagingAndSortingRepository(repositoryInterfaceName)) {
            classCreator.method("list", mc -> {
                mc.public_();
                mc.returning(List.class);
                mc.parameter("page", Page.class);
                mc.parameter("sort", Sort.class);
                mc.parameter("namedQuery", String.class);
                mc.parameter("queryParams", Map.class);
                mc.body(bc -> {
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    Expr result = bc.invokeInterface(LIST_ITERABLE, repository);
                    bc.return_(result);
                });
            });
        }
    }

    //ListCrudRepository List<T> findAll();
    public void implementList(ClassCreator classCreator, String repositoryInterfaceName) {
        if (entityClassHelper.isListCrudRepository(repositoryInterfaceName)
                && !entityClassHelper.isListPagingAndSortingRepository(repositoryInterfaceName)) {
            classCreator.method("list", mc -> {
                mc.public_();
                mc.returning(List.class);
                mc.parameter("page", Page.class);
                mc.parameter("sort", Sort.class);
                mc.parameter("namedQuery", String.class);
                mc.parameter("queryParams", Map.class);
                mc.body(bc -> {
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    Expr result = bc.invokeInterface(LIST, repository);
                    bc.return_(result);
                });
            });
        }
    }

    // PagingAndSortingRepository Page<T> findAll(Pageable pageable);
    // PagingAndSortingRepository Iterable<T> findAll(Pageable pageable);
    // ListPagingAndSortingRepository List<T> findAll(Sort sort);
    public void implementPagedList(ClassCreator classCreator, String repositoryInterfaceName) {
        if (entityClassHelper.isPagingAndSortingRepository(repositoryInterfaceName)) {
            classCreator.method("list", mc -> {
                mc.public_();
                mc.returning(List.class);
                var page = mc.parameter("page", Page.class);
                var sort = mc.parameter("sort", Sort.class);
                mc.parameter("namedQuery", String.class);
                mc.parameter("queryParams", Map.class);
                mc.body(bc -> {
                    LocalVar pageable = bc.localVar("pageable", toPageable(bc, page, sort));
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    Expr resultPage = bc.invokeInterface(LIST_PAGED, repository, pageable);
                    Expr result = bc.invokeInterface(
                            MethodDesc.of(org.springframework.data.domain.Page.class, "getContent", List.class), resultPage);
                    bc.return_(result);
                });
            });
        }
    }

    //PagingAndSortingRepository Page<T> findAll(Pageable pageable);
    public void implementListPageCount(ClassCreator classCreator, String repositoryInterfaceName) {
        classCreator.method(Constants.PAGE_COUNT_METHOD_PREFIX + "list", mc -> {
            mc.public_();
            mc.returning(int.class);
            var page = mc.parameter("page", Page.class);
            mc.parameter("namedQuery", String.class);
            mc.parameter("queryParams", Map.class);
            mc.body(bc -> {
                if (entityClassHelper.isPagingAndSortingRepository(repositoryInterfaceName)) {
                    LocalVar pageable = bc.localVar("pageable", toPageable(bc, page));
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    Expr resultPage = bc.invokeInterface(LIST_PAGED, repository, pageable);
                    Expr pageCount = bc.invokeInterface(
                            MethodDesc.of(org.springframework.data.domain.Page.class, "getTotalPages", int.class),
                            resultPage);
                    bc.return_(pageCount);
                } else {
                    bc.throw_(RuntimeException.class, "Method not implemented");
                }
            });
        });
    }

    //ListCrudRepository List<T> findAllById(Iterable<ID> ids);
    public void implementListById(ClassCreator classCreator, String repositoryInterfaceName) {
        if (entityClassHelper.isListCrudRepository(repositoryInterfaceName)) {
            classCreator.method("list", mc -> {
                mc.public_();
                mc.returning(List.class);
                var ids = mc.parameter("ids", Iterable.class);
                mc.body(bc -> {
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    Expr result = bc.invokeInterface(LIST_BY_ID, repository, ids);
                    bc.return_(result);
                });
            });
        }
    }

    // CrudRepository Optional<T> findById(ID id);
    public void implementGet(ClassCreator classCreator, String repositoryInterfaceName) {
        classCreator.method("get", mc -> {
            mc.public_();
            mc.returning(Object.class);
            var id = mc.parameter("id", Object.class);
            mc.body(bc -> {
                if (entityClassHelper.isCrudRepository(repositoryInterfaceName)) {
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    Expr result = findById(bc, id, repository);
                    bc.return_(result);
                } else {
                    bc.throw_(RuntimeException.class, "Method not implemented");
                }
            });
        });
    }

    // CrudRepository <S extends T> S save(S entity);
    public void implementAdd(ClassCreator classCreator, String repositoryInterfaceName) {
        classCreator.method("add", mc -> {
            mc.public_();
            mc.returning(Object.class);
            var entity = mc.parameter("entity", Object.class);
            mc.body(bc -> {
                if (entityClassHelper.isCrudRepository(repositoryInterfaceName)) {
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    Expr result = bc.invokeInterface(ADD, repository, entity);
                    bc.return_(result);
                } else {
                    bc.throw_(RuntimeException.class, "Method not implemented");
                }
            });
        });
    }

    //ListCrudRepository  List<S> saveAll(Iterable<S> entities);
    public void implementAddList(ClassCreator classCreator, String repositoryInterfaceName) {
        classCreator.method("addAll", mc -> {
            mc.public_();
            mc.returning(List.class);
            var entity = mc.parameter("entities", Iterable.class);
            mc.body(bc -> {
                if (entityClassHelper.isListCrudRepository(repositoryInterfaceName)) {
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    Expr result = bc.invokeInterface(SAVE_LIST, repository, entity);
                    bc.return_(result);
                } else {
                    bc.throw_(RuntimeException.class, "Method not implemented");
                }
            });
        });
    }

    public void implementUpdate(ClassCreator classCreator, String repositoryInterfaceName, String entityType) {
        classCreator.method("update", mc -> {
            mc.public_();
            mc.returning(Object.class);
            var id = mc.parameter("id", Object.class);
            var entity = mc.parameter("entity", Object.class);
            mc.body(bc -> {
                if (entityClassHelper.isCrudRepository(repositoryInterfaceName)) {
                    // Set entity ID before executing an update to make sure that a requested object ID matches a given entity ID.
                    setId(bc, entityType, entity, id);
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    Expr result = bc.invokeInterface(UPDATE, repository, entity);
                    bc.return_(result);
                } else {
                    bc.throw_(RuntimeException.class, "Method not implemented");
                }
            });
        });
    }

    public void implementDelete(ClassCreator classCreator, String repositoryInterfaceName) {
        classCreator.method("delete", mc -> {
            mc.public_();
            mc.returning(boolean.class);
            var id = mc.parameter("id", Object.class);
            mc.body(bc -> {
                if (entityClassHelper.isCrudRepository(repositoryInterfaceName)) {
                    Expr repository = getRepositoryInstance(bc, repositoryInterfaceName);
                    LocalVar entity = bc.localVar("entity", findById(bc, id, repository));
                    LocalVar result = bc.localVar("result", boolean.class, Const.of(false));
                    bc.if_(bc.isNotNull(entity), trueBranch -> {
                        trueBranch.invokeInterface(DELETE, repository, id);
                        trueBranch.set(result, Const.of(true));
                    });
                    bc.return_(result);
                } else {
                    bc.throw_(RuntimeException.class, "Method not implemented");
                }
            });
        });
    }

    private Expr findById(BlockCreator bc, Expr id, Expr repository) {
        Expr optional = bc.invokeInterface(GET, repository, id);
        return bc.invokeVirtual(MethodDesc.of(Optional.class, "orElse", Object.class, Object.class),
                optional, Const.ofNull(Object.class));
    }

    private void setId(BlockCreator bc, String entityType, Expr entity, Expr id) {
        FieldInfo idField = entityClassHelper.getIdField(entityType);
        MethodDesc idSetter = entityClassHelper.getSetter(entityType, idField);
        bc.invokeVirtual(idSetter, entity, id);
    }

    /**
     * <pre>
     * Pageable toPageable(Page panachePage) {
     *     return PageRequest.of(panachePage.index, panachePage.size);
     * }
     * </pre>
     */
    private Expr toPageable(BlockCreator bc, Expr panachePage) {
        return bc.invokeStatic(
                MethodDesc.of(PageRequest.class, "of", PageRequest.class, int.class, int.class),
                bc.get(panachePage.field(FieldDesc.of(PANACHE_PAGE, "index"))),
                bc.get(panachePage.field(FieldDesc.of(PANACHE_PAGE, "size"))));
    }

    /**
     * <pre>
     * Pageable toPageable(Page panachePage, io.quarkus.panache.common.Sort panacheSort) {
     *     Sort springSort = toSpringSort(panacheSort);
     *     return PageRequest.of(panachePage.index, panachePage.size, springSort);
     * }
     * </pre>
     */
    private Expr toPageable(BlockCreator bc, Expr panachePage, Expr panacheSort) {
        return bc.invokeStatic(
                MethodDesc.of(PageRequest.class, "of", PageRequest.class, int.class, int.class,
                        org.springframework.data.domain.Sort.class),
                bc.get(panachePage.field(FieldDesc.of(PANACHE_PAGE, "index"))),
                bc.get(panachePage.field(FieldDesc.of(PANACHE_PAGE, "size"))),
                toSpringSort(bc, panacheSort));
    }

    /**
     * <pre>
     * Sort toSpringSort(io.quarkus.panache.common.Sort sort) {
     *     Sort springSort;
     *     springSort = Sort.unsorted();
     *     List<io.quarkus.panache.common.Sort.Column> columns = sort.getColumns();
     *     Iterator<io.quarkus.panache.common.Sort.Column> columnsIterator = columns.iterator();
     *     while (columnsIterator.hasNext()) {
     *         io.quarkus.panache.common.Sort.Column column = columnsIterator.next();
     *         Sort.Order[] orderArray = new Sort.Order[1];
     *         String columnName = column.getName();
     *         io.quarkus.panache.common.Sort.Direction direction = column.getDirection();
     *         io.quarkus.panache.common.Sort.Direction ascending = io.quarkus.panache.common.Sort.Direction
     *                 .valueOf("Ascending");
     *         if (ascending.equals(direction)) {
     *             orderArray[0] = Sort.Order.asc(columnName);
     *         } else {
     *             orderArray[0] = Sort.Order.desc(columnName);
     *         }
     *         Sort subSort = Sort.by(orderArray);
     *         springSort = subSort.and(subSort);
     *     }
     *     return springSort;
     * }
     * </pre>
     */
    private Expr toSpringSort(BlockCreator bc, Expr panacheSort) {
        LocalVar springSort = bc.localVar("springSort", org.springframework.data.domain.Sort.class,
                bc.invokeStatic(MethodDesc.of(org.springframework.data.domain.Sort.class, "unsorted",
                        org.springframework.data.domain.Sort.class)));

        // Loop through the columns
        Expr columns = bc.invokeVirtual(
                MethodDesc.of(PANACHE_SORT, "getColumns", List.class), panacheSort);
        LocalVar columnsIterator = bc.localVar("columnsIterator",
                bc.invokeInterface(MethodDesc.of(List.class, "iterator", Iterator.class), columns));

        bc.while_(
                cond -> {
                    Expr hasNext = cond.invokeInterface(
                            MethodDesc.of(Iterator.class, "hasNext", boolean.class), columnsIterator);
                    cond.yield(hasNext);
                },
                body -> {
                    LocalVar column = body.localVar("column", body.invokeInterface(
                            MethodDesc.of(Iterator.class, "next", Object.class), columnsIterator));
                    addColumn(body, springSort, column);
                });

        return springSort;
    }

    private void addColumn(BlockCreator bc, LocalVar springSort, Expr column) {
        LocalVar columnName = bc.localVar("columnName", bc.invokeVirtual(
                MethodDesc.of(PANACHE_COLUMN, "getName", String.class), column));
        LocalVar direction = bc.localVar("direction", bc.invokeVirtual(
                MethodDesc.of(PANACHE_COLUMN, "getDirection", PANACHE_DIRECTION), column));

        LocalVar isAscending = bc.localVar("isAscending", bc.invokeVirtual(
                MethodDesc.of(PANACHE_DIRECTION, "equals", boolean.class, Object.class),
                bc.invokeStatic(
                        MethodDesc.of(PANACHE_DIRECTION, "valueOf", PANACHE_DIRECTION, String.class),
                        Const.of("Ascending")),
                direction));

        LocalVar orderExpr = bc.localVar("order", org.springframework.data.domain.Sort.Order.class,
                bc.invokeStatic(
                        MethodDesc.of(org.springframework.data.domain.Sort.Order.class, "desc",
                                org.springframework.data.domain.Sort.Order.class, String.class),
                        columnName));
        bc.if_(isAscending, trueBranch -> {
            trueBranch.set(orderExpr, trueBranch.invokeStatic(
                    MethodDesc.of(org.springframework.data.domain.Sort.Order.class, "asc",
                            org.springframework.data.domain.Sort.Order.class, String.class),
                    columnName));
        });

        Expr orderArray = bc.newArray(org.springframework.data.domain.Sort.Order.class, orderExpr);
        Expr subSort = bc.invokeStatic(
                MethodDesc.of(org.springframework.data.domain.Sort.class, "by", org.springframework.data.domain.Sort.class,
                        org.springframework.data.domain.Sort.Order[].class),
                orderArray);
        bc.set(springSort, bc.invokeVirtual(
                MethodDesc.of(org.springframework.data.domain.Sort.class, "and", org.springframework.data.domain.Sort.class,
                        org.springframework.data.domain.Sort.class),
                springSort, subSort));
    }

}
