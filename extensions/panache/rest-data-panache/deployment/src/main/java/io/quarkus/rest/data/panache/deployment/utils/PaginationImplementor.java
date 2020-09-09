package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.panache.common.Page;

public final class PaginationImplementor {

    /**
     * Extracts page and size query parameters from the URI and returns the {@link Page} instance.
     * If page index or size is invalid - default value is used.
     *
     * @param creator a bytecode creator to be used for code generation
     * @param uriInfo a {@link UriInfo} instance to extract the query parameters form
     * @return a {@link Page} instance
     */
    public ResultHandle getRequestPage(BytecodeCreator creator, ResultHandle uriInfo) {
        ResultHandle queryParams = creator.invokeInterfaceMethod(
                ofMethod(UriInfo.class, "getQueryParameters", MultivaluedMap.class), uriInfo);
        AssignableResultHandle page = creator.createVariable(Integer.class);
        assignIntQueryParam(creator, queryParams, "page", 0, 0, page);
        AssignableResultHandle size = creator.createVariable(Integer.class);
        assignIntQueryParam(creator, queryParams, "size", 1, 20, size);
        return creator.invokeStaticMethod(ofMethod(Page.class, "of", Page.class, int.class, int.class), page, size);
    }

    private void assignIntQueryParam(BytecodeCreator creator, ResultHandle queryParams, String key, int minValue,
            int defaultValue, AssignableResultHandle variable) {
        ResultHandle stringValue = creator.invokeInterfaceMethod(
                ofMethod(MultivaluedMap.class, "getFirst", Object.class, Object.class), queryParams, creator.load(key));
        TryBlock tryBlock = creator.tryBlock();

        // Catch NumberFormatException and return default
        CatchBlockCreator catchBlockCreator = tryBlock.addCatch(NumberFormatException.class);
        catchBlockCreator.assign(variable, catchBlockCreator.load(defaultValue));

        // Parse int and return that or default
        ResultHandle value = tryBlock.invokeStaticMethod(
                ofMethod(Integer.class, "parseInt", int.class, String.class), stringValue);
        BranchResult valueIsTooSmall = tryBlock.ifIntegerLessThan(value, tryBlock.load(minValue));
        valueIsTooSmall.trueBranch().assign(variable, tryBlock.load(defaultValue));
        valueIsTooSmall.falseBranch().assign(variable, value);
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
                ofMethod(Integer.class, "sum", int.class, int.class, int.class), pageCount, creator.load(-1));
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
