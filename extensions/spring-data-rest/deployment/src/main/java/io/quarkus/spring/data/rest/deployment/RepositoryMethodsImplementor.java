package io.quarkus.spring.data.rest.deployment;

import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.deployment.Constants;

public class RepositoryMethodsImplementor implements ResourceMethodsImplementor {

    private static final Logger LOGGER = Logger.getLogger(RepositoryMethodsImplementor.class);

    //CrudRepository
    public static final MethodDescriptor GET = ofMethod(CrudRepository.class, "findById", Optional.class, Object.class);
    public static final MethodDescriptor ADD = ofMethod(CrudRepository.class, "save", Object.class, Object.class);
    public static final MethodDescriptor UPDATE = ofMethod(CrudRepository.class, "save", Object.class, Object.class);

    public static final MethodDescriptor DELETE = ofMethod(CrudRepository.class, "deleteById", void.class, Object.class);
    public static final MethodDescriptor LIST_ITERABLE = ofMethod(CrudRepository.class, "findAll", Iterable.class);

    //ListCrudRepository
    public static final MethodDescriptor LIST = ofMethod(ListCrudRepository.class, "findAll", List.class);
    public static final MethodDescriptor LIST_BY_ID = ofMethod(ListCrudRepository.class, "findAllById", List.class,
            Iterable.class);
    public static final MethodDescriptor SAVE_LIST = ofMethod(ListCrudRepository.class, "saveAll", List.class, Iterable.class);

    //PagingAndSortingRepository
    public static final MethodDescriptor LIST_PAGED = ofMethod(PagingAndSortingRepository.class, "findAll",
            org.springframework.data.domain.Page.class, Pageable.class);

    private static final Class<?> PANACHE_PAGE = io.quarkus.panache.common.Page.class;

    private static final Class<?> PANACHE_SORT = io.quarkus.panache.common.Sort.class;

    private static final Class<?> PANACHE_COLUMN = io.quarkus.panache.common.Sort.Column.class;

    private static final Class<?> PANACHE_DIRECTION = io.quarkus.panache.common.Sort.Direction.class;

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
            MethodCreator methodCreator = classCreator.getMethodCreator("list", List.class, Page.class, Sort.class,
                    String.class, Map.class);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle result = methodCreator.invokeInterfaceMethod(LIST_ITERABLE, repository);
            methodCreator.returnValue(result);
            LOGGER.debugf("Method code: %s ", methodCreator.getMethodDescriptor().toString());
            methodCreator.close();
        }
    }

    //ListCrudRepository List<T> findAll();
    public void implementList(ClassCreator classCreator, String repositoryInterfaceName) {
        if (entityClassHelper.isListCrudRepository(repositoryInterfaceName)
                && !entityClassHelper.isListPagingAndSortingRepository(repositoryInterfaceName)) {
            MethodCreator methodCreator = classCreator.getMethodCreator("list", List.class, Page.class, Sort.class,
                    String.class, Map.class);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle result = methodCreator.invokeInterfaceMethod(LIST, repository);
            methodCreator.returnValue(result);
            LOGGER.debugf("Method code: %s ", methodCreator.toString());
            methodCreator.close();
        }
    }

    // PagingAndSortingRepository Page<T> findAll(Pageable pageable);
    // PagingAndSortingRepository Iterable<T> findAll(Pageable pageable);
    // ListPagingAndSortingRepository List<T> findAll(Sort sort);
    public void implementPagedList(ClassCreator classCreator, String repositoryInterfaceName) {
        if (entityClassHelper.isPagingAndSortingRepository(repositoryInterfaceName)) {
            MethodCreator methodCreator = classCreator.getMethodCreator("list", List.class, Page.class,
                    io.quarkus.panache.common.Sort.class, String.class, Map.class);

            ResultHandle page = methodCreator.getMethodParam(0);
            ResultHandle sort = methodCreator.getMethodParam(1);
            ResultHandle pageable = toPageable(methodCreator, page, sort);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle resultPage = methodCreator.invokeInterfaceMethod(LIST_PAGED, repository, pageable);
            ResultHandle result = methodCreator.invokeInterfaceMethod(
                    ofMethod(org.springframework.data.domain.Page.class, "getContent", List.class), resultPage);

            methodCreator.returnValue(result);
            LOGGER.debugf("Method code: %s ", methodCreator.toString());
            methodCreator.close();
        }
    }

    //PagingAndSortingRepository Page<T> findAll(Pageable pageable);
    public void implementListPageCount(ClassCreator classCreator, String repositoryInterfaceName) {
        MethodCreator methodCreator = classCreator.getMethodCreator(Constants.PAGE_COUNT_METHOD_PREFIX + "list",
                int.class, Page.class, String.class, Map.class);
        if (entityClassHelper.isPagingAndSortingRepository(repositoryInterfaceName)) {
            ResultHandle page = methodCreator.getMethodParam(0);
            ResultHandle pageable = toPageable(methodCreator, page);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle resultPage = methodCreator.invokeInterfaceMethod(LIST_PAGED, repository, pageable);
            ResultHandle pageCount = methodCreator.invokeInterfaceMethod(
                    ofMethod(org.springframework.data.domain.Page.class, "getTotalPages", int.class), resultPage);
            methodCreator.returnValue(pageCount);
        } else {
            methodCreator.throwException(RuntimeException.class, "Method not implemented");
        }
        LOGGER.debugf("Method code: %s ", methodCreator.toString());
        methodCreator.close();
    }

    //ListCrudRepository List<T> findAllById(Iterable<ID> ids);
    public void implementListById(ClassCreator classCreator, String repositoryInterfaceName) {
        if (entityClassHelper.isListCrudRepository(repositoryInterfaceName)) {
            MethodCreator methodCreator = classCreator.getMethodCreator("list", List.class, Iterable.class);
            ResultHandle ids = methodCreator.getMethodParam(0);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle result = methodCreator.invokeInterfaceMethod(LIST_BY_ID, repository, ids);
            methodCreator.returnValue(result);
            LOGGER.debugf("Method code: %s ", methodCreator.toString());
            methodCreator.close();
        }
    }

    // CrudRepository Optional<T> findById(ID id);
    public void implementGet(ClassCreator classCreator, String repositoryInterfaceName) {
        MethodCreator methodCreator = classCreator.getMethodCreator("get", Object.class, Object.class);
        if (entityClassHelper.isCrudRepository(repositoryInterfaceName)) {
            ResultHandle id = methodCreator.getMethodParam(0);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle result = findById(methodCreator, id, repository);
            methodCreator.returnValue(result);
        } else {
            methodCreator.throwException(RuntimeException.class, "Method not implemented");
        }
        LOGGER.debugf("Method code: %s ", methodCreator.toString());
        methodCreator.close();
    }

    // CrudRepository <S extends T> S save(S entity);
    public void implementAdd(ClassCreator classCreator, String repositoryInterfaceName) {
        MethodCreator methodCreator = classCreator.getMethodCreator("add", Object.class, Object.class);
        if (entityClassHelper.isCrudRepository(repositoryInterfaceName)) {
            ResultHandle entity = methodCreator.getMethodParam(0);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle result = methodCreator.invokeInterfaceMethod(ADD, repository, entity);

            methodCreator.returnValue(result);
        } else {
            methodCreator.throwException(RuntimeException.class, "Method not implemented");

        }
        LOGGER.debugf("Method code: %s ", methodCreator.toString());
        methodCreator.close();
    }

    //ListCrudRepository  List<S> saveAll(Iterable<S> entities);
    public void implementAddList(ClassCreator classCreator, String repositoryInterfaceName) {
        MethodCreator methodCreator = classCreator.getMethodCreator("addAll", List.class, Iterable.class);
        if (entityClassHelper.isListCrudRepository(repositoryInterfaceName)) {
            ResultHandle entity = methodCreator.getMethodParam(0);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle result = methodCreator.invokeInterfaceMethod(SAVE_LIST, repository, entity);
            methodCreator.returnValue(result);
        } else {
            methodCreator.throwException(RuntimeException.class, "Method not implemented");
        }
        LOGGER.debugf("Method code: %s ", methodCreator.toString());
        methodCreator.close();
    }

    public void implementUpdate(ClassCreator classCreator, String repositoryInterfaceName, String entityType) {
        MethodCreator methodCreator = classCreator.getMethodCreator("update", Object.class, Object.class, Object.class);
        if (entityClassHelper.isCrudRepository(repositoryInterfaceName)) {
            ResultHandle id = methodCreator.getMethodParam(0);
            ResultHandle entity = methodCreator.getMethodParam(1);
            // Set entity ID before executing an update to make sure that a requested object ID matches a given entity ID.
            setId(methodCreator, entityType, entity, id);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle result = methodCreator.invokeInterfaceMethod(UPDATE, repository, entity);
            methodCreator.returnValue(result);
        } else {
            methodCreator.throwException(RuntimeException.class, "Method not implemented");
        }
        LOGGER.debugf("Method code: %s ", methodCreator.toString());
        methodCreator.close();
    }

    public void implementDelete(ClassCreator classCreator, String repositoryInterfaceName) {
        MethodCreator methodCreator = classCreator.getMethodCreator("delete", boolean.class, Object.class);

        if (entityClassHelper.isCrudRepository(repositoryInterfaceName)) {
            ResultHandle id = methodCreator.getMethodParam(0);
            ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterfaceName);
            ResultHandle entity = findById(methodCreator, id, repository);
            AssignableResultHandle result = methodCreator.createVariable(boolean.class);
            BranchResult entityExists = methodCreator.ifNotNull(entity);
            entityExists.trueBranch().invokeInterfaceMethod(DELETE, repository, id);
            entityExists.trueBranch().assign(result, entityExists.trueBranch().load(true));
            entityExists.falseBranch().assign(result, entityExists.falseBranch().load(false));

            methodCreator.returnValue(result);
        } else {
            methodCreator.throwException(RuntimeException.class, "Method not implemented");
        }
        LOGGER.debugf("Method code: %s ", methodCreator.toString());
        methodCreator.close();
    }

    private ResultHandle findById(BytecodeCreator creator, ResultHandle id, ResultHandle repository) {
        ResultHandle optional = creator.invokeInterfaceMethod(GET, repository, id);
        return creator.invokeVirtualMethod(ofMethod(Optional.class, "orElse", Object.class, Object.class),
                optional, creator.loadNull());
    }

    private void setId(BytecodeCreator creator, String entityType, ResultHandle entity, ResultHandle id) {
        FieldInfo idField = entityClassHelper.getIdField(entityType);
        MethodDescriptor idSetter = entityClassHelper.getSetter(entityType, idField);
        creator.invokeVirtualMethod(idSetter, entity, id);
    }

    /**
     * <pre>
     * Pageable toPageable(Page panachePage) {
     *     return PageRequest.of(panachePage.index, panachePage.size);
     * }
     * </pre>
     */
    private ResultHandle toPageable(MethodCreator creator, ResultHandle panachePage) {
        ResultHandle index = creator.readInstanceField(of(PANACHE_PAGE, "index", int.class), panachePage);
        ResultHandle size = creator.readInstanceField(of(PANACHE_PAGE, "size", int.class), panachePage);
        return creator.invokeStaticMethod(
                ofMethod(PageRequest.class, "of", PageRequest.class, int.class, int.class), index, size);
    }

    /**
     * <pre>
     * Pageable toPageable(Page panachePage, io.quarkus.panache.common.Sort panacheSort) {
     *     Sort springSort = toSpringSort(panacheSort);
     *     return PageRequest.of(panachePage.index, panachePage.size, springSort);
     * }
     * </pre>
     */
    private ResultHandle toPageable(MethodCreator creator, ResultHandle panachePage, ResultHandle panacheSort) {
        ResultHandle index = creator.readInstanceField(of(PANACHE_PAGE, "index", int.class), panachePage);
        ResultHandle size = creator.readInstanceField(of(PANACHE_PAGE, "size", int.class), panachePage);
        ResultHandle springSort = toSpringSort(creator, panacheSort);
        return creator.invokeStaticMethod(
                ofMethod(PageRequest.class, "of", PageRequest.class, int.class, int.class,
                        org.springframework.data.domain.Sort.class),
                index, size, springSort);
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
    private ResultHandle toSpringSort(MethodCreator creator, ResultHandle panacheSort) {
        AssignableResultHandle springSort = creator.createVariable(org.springframework.data.domain.Sort.class);
        creator.assign(springSort, creator.invokeStaticMethod(
                ofMethod(org.springframework.data.domain.Sort.class, "unsorted", org.springframework.data.domain.Sort.class)));

        // Loop through the columns
        ResultHandle columns = creator.invokeVirtualMethod(
                ofMethod(PANACHE_SORT, "getColumns", List.class), panacheSort);
        ResultHandle columnsIterator = creator.invokeInterfaceMethod(
                ofMethod(List.class, "iterator", Iterator.class), columns);
        BytecodeCreator loopCreator = creator.whileLoop(c -> iteratorHasNext(c, columnsIterator)).block();
        ResultHandle column = loopCreator.invokeInterfaceMethod(
                ofMethod(Iterator.class, "next", Object.class), columnsIterator);
        addColumn(loopCreator, springSort, column);

        return springSort;
    }

    private BranchResult iteratorHasNext(BytecodeCreator creator, ResultHandle iterator) {
        return creator.ifTrue(
                creator.invokeInterfaceMethod(ofMethod(Iterator.class, "hasNext", boolean.class), iterator));
    }

    private void addColumn(BytecodeCreator creator, AssignableResultHandle springSort, ResultHandle column) {
        ResultHandle orderArray = creator.newArray(org.springframework.data.domain.Sort.Order.class, 1);
        setOrder(creator, orderArray, column);
        ResultHandle subSort = creator.invokeStaticMethod(
                ofMethod(org.springframework.data.domain.Sort.class, "by", org.springframework.data.domain.Sort.class,
                        org.springframework.data.domain.Sort.Order[].class),
                orderArray);
        creator.assign(springSort, creator.invokeVirtualMethod(
                ofMethod(org.springframework.data.domain.Sort.class, "and", org.springframework.data.domain.Sort.class,
                        org.springframework.data.domain.Sort.class),
                springSort, subSort));
    }

    private void setOrder(BytecodeCreator creator, ResultHandle orderArray, ResultHandle column) {
        ResultHandle columnName = creator.invokeVirtualMethod(
                ofMethod(PANACHE_COLUMN, "getName", String.class), column);
        ResultHandle direction = creator.invokeVirtualMethod(
                ofMethod(PANACHE_COLUMN, "getDirection", PANACHE_DIRECTION), column);
        BranchResult isAscendingBranch = isAscending(creator, direction);
        isAscendingBranch.trueBranch()
                .writeArrayValue(orderArray, 0, isAscendingBranch.trueBranch().invokeStaticMethod(
                        ofMethod(org.springframework.data.domain.Sort.Order.class, "asc",
                                org.springframework.data.domain.Sort.Order.class, String.class),
                        columnName));
        isAscendingBranch.falseBranch()
                .writeArrayValue(orderArray, 0, isAscendingBranch.falseBranch().invokeStaticMethod(
                        ofMethod(org.springframework.data.domain.Sort.Order.class, "desc",
                                org.springframework.data.domain.Sort.Order.class, String.class),
                        columnName));
    }

    private BranchResult isAscending(BytecodeCreator creator, ResultHandle panacheDirection) {
        ResultHandle ascending = creator.invokeStaticMethod(
                ofMethod(PANACHE_DIRECTION, "valueOf", PANACHE_DIRECTION, String.class), creator.load("Ascending"));
        return creator.ifTrue(creator.invokeVirtualMethod(
                ofMethod(PANACHE_DIRECTION, "equals", boolean.class, Object.class), ascending, panacheDirection));
    }

}
