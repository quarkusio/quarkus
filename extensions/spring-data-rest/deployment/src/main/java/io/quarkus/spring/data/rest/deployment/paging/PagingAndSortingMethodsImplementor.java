package io.quarkus.spring.data.rest.deployment.paging;

import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.Iterator;
import java.util.List;

import org.jboss.jandex.IndexView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.common.Page;
import io.quarkus.rest.data.panache.deployment.Constants;
import io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor;

public class PagingAndSortingMethodsImplementor extends CrudMethodsImplementor {

    public static final MethodDescriptor LIST_PAGED = ofMethod(PagingAndSortingRepository.class, "findAll",
            org.springframework.data.domain.Page.class, Pageable.class);

    private static final Class<?> PANACHE_PAGE = io.quarkus.panache.common.Page.class;

    private static final Class<?> PANACHE_SORT = io.quarkus.panache.common.Sort.class;

    private static final Class<?> PANACHE_COLUMN = io.quarkus.panache.common.Sort.Column.class;

    private static final Class<?> PANACHE_DIRECTION = io.quarkus.panache.common.Sort.Direction.class;

    public PagingAndSortingMethodsImplementor(IndexView index) {
        super(index);
    }

    public void implementList(ClassCreator classCreator, String repositoryInterface) {
        MethodCreator methodCreator = classCreator.getMethodCreator("list", List.class, Page.class,
                io.quarkus.panache.common.Sort.class);

        ResultHandle page = methodCreator.getMethodParam(0);
        ResultHandle sort = methodCreator.getMethodParam(1);
        ResultHandle pageable = toPageable(methodCreator, page, sort);
        ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterface);
        ResultHandle resultPage = methodCreator.invokeInterfaceMethod(LIST_PAGED, repository, pageable);
        ResultHandle result = methodCreator.invokeInterfaceMethod(
                ofMethod(org.springframework.data.domain.Page.class, "getContent", List.class), resultPage);

        methodCreator.returnValue(result);
        methodCreator.close();
    }

    public void implementListPageCount(ClassCreator classCreator, String repositoryInterface) {
        MethodCreator methodCreator = classCreator.getMethodCreator(Constants.PAGE_COUNT_METHOD_PREFIX + "list",
                int.class, Page.class);
        ResultHandle page = methodCreator.getMethodParam(0);
        ResultHandle pageable = toPageable(methodCreator, page);
        ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterface);
        ResultHandle resultPage = methodCreator.invokeInterfaceMethod(LIST_PAGED, repository, pageable);
        ResultHandle pageCount = methodCreator.invokeInterfaceMethod(
                ofMethod(org.springframework.data.domain.Page.class, "getTotalPages", int.class), resultPage);

        methodCreator.returnValue(pageCount);
        methodCreator.close();
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
                ofMethod(PageRequest.class, "of", PageRequest.class, int.class, int.class, Sort.class),
                index, size, springSort);
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
        AssignableResultHandle springSort = creator.createVariable(Sort.class);
        creator.assign(springSort, creator.invokeStaticMethod(ofMethod(Sort.class, "unsorted", Sort.class)));

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
        ResultHandle orderArray = creator.newArray(Sort.Order.class, 1);
        setOrder(creator, orderArray, column);
        ResultHandle subSort = creator.invokeStaticMethod(
                ofMethod(Sort.class, "by", Sort.class, Sort.Order[].class), orderArray);
        creator.assign(springSort, creator.invokeVirtualMethod(
                ofMethod(Sort.class, "and", Sort.class, Sort.class), springSort, subSort));
    }

    private void setOrder(BytecodeCreator creator, ResultHandle orderArray, ResultHandle column) {
        ResultHandle columnName = creator.invokeVirtualMethod(
                ofMethod(PANACHE_COLUMN, "getName", String.class), column);
        ResultHandle direction = creator.invokeVirtualMethod(
                ofMethod(PANACHE_COLUMN, "getDirection", PANACHE_DIRECTION), column);
        BranchResult isAscendingBranch = isAscending(creator, direction);
        isAscendingBranch.trueBranch()
                .writeArrayValue(orderArray, 0, isAscendingBranch.trueBranch().invokeStaticMethod(
                        ofMethod(Sort.Order.class, "asc", Sort.Order.class, String.class), columnName));
        isAscendingBranch.falseBranch()
                .writeArrayValue(orderArray, 0, isAscendingBranch.falseBranch().invokeStaticMethod(
                        ofMethod(Sort.Order.class, "desc", Sort.Order.class, String.class), columnName));
    }

    private BranchResult isAscending(BytecodeCreator creator, ResultHandle panacheDirection) {
        ResultHandle ascending = creator.invokeStaticMethod(
                ofMethod(PANACHE_DIRECTION, "valueOf", PANACHE_DIRECTION, String.class), creator.load("Ascending"));
        return creator.ifTrue(creator.invokeVirtualMethod(
                ofMethod(PANACHE_DIRECTION, "equals", boolean.class, Object.class), ascending, panacheDirection));
    }
}
