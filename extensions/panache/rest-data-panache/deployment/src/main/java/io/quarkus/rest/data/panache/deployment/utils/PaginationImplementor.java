package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.common.Page;

/**
 * Pagination logic implementor utilities.
 */
public final class PaginationImplementor {

    public static final int DEFAULT_PAGE_INDEX = 0;

    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Get a {@link Page} instance give an index and size.
     */
    public ResultHandle getPage(BytecodeCreator creator, ResultHandle index, ResultHandle size) {
        ResultHandle validIndex = getValidOrDefault(creator, index, 0, DEFAULT_PAGE_INDEX);
        ResultHandle validSize = getValidOrDefault(creator, size, 1, DEFAULT_PAGE_SIZE);
        return creator.invokeStaticMethod(ofMethod(Page.class, "of", Page.class, int.class, int.class), validIndex, validSize);
    }

    private ResultHandle getValidOrDefault(BytecodeCreator creator, ResultHandle value, int minValue, int defaultValue) {
        AssignableResultHandle result = creator.createVariable(int.class);
        BranchResult isValid = creator.ifIntegerGreaterEqual(value, creator.load(minValue));
        isValid.trueBranch().assign(result, value);
        isValid.falseBranch().assign(result, isValid.falseBranch().load(defaultValue));
        return result;
    }

    /**
     * Return an array with the links applicable for the provided page and page count.
     */
    public ResultHandle getLinks(BytecodeCreator creator, ResultHandle uriInfo, ResultHandle page,
            ResultHandle pageCount) {
        ResultHandle links = creator.newInstance(ofConstructor(ArrayList.class, int.class), creator.load(4));

        ResultHandle firstPage = creator.invokeVirtualMethod(ofMethod(Page.class, "first", Page.class), page);
        ResultHandle firstPageLink = getLink(creator, uriInfo, firstPage, "first");
        creator.invokeInterfaceMethod(ofMethod(List.class, "add", boolean.class, Object.class), links, firstPageLink);

        ResultHandle lastPage = getLastPage(creator, page, pageCount);
        ResultHandle lastPageLink = getLink(creator, uriInfo, lastPage, "last");
        creator.invokeInterfaceMethod(ofMethod(List.class, "add", boolean.class, Object.class), links, lastPageLink);

        BytecodeCreator previousPageCreator = isTheSamePage(creator, page, firstPage).falseBranch();
        ResultHandle previousPage = previousPageCreator.invokeVirtualMethod(ofMethod(Page.class, "previous", Page.class), page);
        ResultHandle previousPageLink = getLink(previousPageCreator, uriInfo, previousPage, "previous");
        previousPageCreator.invokeInterfaceMethod(
                ofMethod(List.class, "add", boolean.class, Object.class), links, previousPageLink);

        BytecodeCreator nextPageCreator = isTheSamePage(creator, page, lastPage).falseBranch();
        ResultHandle nextPage = nextPageCreator.invokeVirtualMethod(ofMethod(Page.class, "next", Page.class), page);
        ResultHandle nextPageLink = getLink(nextPageCreator, uriInfo, nextPage, "next");
        nextPageCreator.invokeInterfaceMethod(ofMethod(List.class, "add", boolean.class, Object.class), links, nextPageLink);

        ResultHandle linksCount = creator.invokeInterfaceMethod(ofMethod(List.class, "size", int.class), links);
        ResultHandle linksArray = creator.newArray(Link.class, linksCount);
        return creator.invokeInterfaceMethod(
                ofMethod(List.class, "toArray", Object[].class, Object[].class), links, linksArray);
    }

    private ResultHandle getLink(BytecodeCreator creator, ResultHandle uriInfo, ResultHandle page, String rel) {
        ResultHandle builder = creator.invokeStaticMethod(
                ofMethod(Link.class, "fromUri", Link.Builder.class, URI.class), getPageUri(creator, uriInfo, page));
        creator.invokeInterfaceMethod(ofMethod(Link.Builder.class, "rel", Link.Builder.class, String.class),
                builder, creator.load(rel));
        return creator.invokeInterfaceMethod(ofMethod(Link.Builder.class, "build", Link.class, Object[].class),
                builder, creator.newArray(Object.class, 0));
    }

    /**
     * Build a {@link URI} for the given page. Takes the absolute path from the given {@link UriInfo} and appends page and size
     * query parameters from the given {@link Page}.
     *
     * @param creator a bytecode creator to be used for code generation
     * @param uriInfo a {@link UriInfo} to be used for the absolute path extraction
     * @param page a {@link Page} to be used for getting page number and size
     * @return a page {@link URI}
     */
    private ResultHandle getPageUri(BytecodeCreator creator, ResultHandle uriInfo, ResultHandle page) {
        ResultHandle uriBuilder = creator.invokeInterfaceMethod(
                ofMethod(UriInfo.class, "getAbsolutePathBuilder", UriBuilder.class), uriInfo);

        // Add page query parameter
        ResultHandle index = creator.readInstanceField(FieldDescriptor.of(Page.class, "index", int.class), page);
        creator.invokeVirtualMethod(
                ofMethod(UriBuilder.class, "queryParam", UriBuilder.class, String.class, Object[].class),
                uriBuilder, creator.load("page"), creator.marshalAsArray(Object.class, index));

        // Add size query parameter
        ResultHandle size = creator.readInstanceField(FieldDescriptor.of(Page.class, "size", int.class), page);
        creator.invokeVirtualMethod(
                ofMethod(UriBuilder.class, "queryParam", UriBuilder.class, String.class, Object[].class),
                uriBuilder, creator.load("size"), creator.marshalAsArray(Object.class, size));

        return creator.invokeVirtualMethod(
                ofMethod(UriBuilder.class, "build", URI.class, Object[].class), uriBuilder, creator.newArray(Object.class, 0));
    }

    /**
     * Returns the last page with the same size as the provided page.
     */
    private ResultHandle getLastPage(BytecodeCreator creator, ResultHandle page, ResultHandle pageCount) {
        ResultHandle pageNumber = creator.invokeStaticMethod(
                ofMethod(Math.class, "max", int.class, int.class, int.class),
                creator.invokeStaticMethod(
                        ofMethod(Integer.class, "sum", int.class, int.class, int.class),
                        pageCount,
                        creator.load(-1)),
                creator.load(0));
        return creator.invokeVirtualMethod(
                ofMethod(Page.class, "index", Page.class, int.class), page, pageNumber);
    }

    /**
     * Compares indexes of two pages.
     */
    private BranchResult isTheSamePage(BytecodeCreator creator, ResultHandle page, ResultHandle anotherPage) {
        ResultHandle index = creator.readInstanceField(FieldDescriptor.of(Page.class, "index", int.class), page);
        ResultHandle anotherIndex = creator.readInstanceField(FieldDescriptor.of(Page.class, "index", int.class), anotherPage);
        return creator.ifIntegerEqual(index, anotherIndex);
    }
}
