package io.quarkus.rest.data.panache.runtime.sort;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SortQueryParamFilterTest {

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private UriInfo uriInfo;

    private final SortQueryParamFilter filter = new SortQueryParamFilter();

    @BeforeEach
    void setUp() {
        given(requestContext.getUriInfo()).willReturn(uriInfo);
    }

    @Test
    void shouldAllowValidParameters() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "$name");
        map.putSingle("sort", "-surname_1");
        given(uriInfo.getQueryParameters()).willReturn(map);

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldAllowValidParametersWithMultipleValues() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "$name,-surname_1");
        given(uriInfo.getQueryParameters()).willReturn(map);

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldAllowEmptyParameters() {
        given(uriInfo.getQueryParameters()).willReturn(new MultivaluedHashMap<>());

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldCatchInvalidParameter() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "$name");
        map.putSingle("sort", "(surname_1");
        given(uriInfo.getQueryParameters()).willReturn(map);

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher("(surname_1")));
    }

    @Test
    void shouldCatchInvalidParameterValue() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "$name,1_surname");
        given(uriInfo.getQueryParameters()).willReturn(map);

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher("$name,1_surname")));
    }

    private ArgumentMatcher<Response> abortResponseMatcher(String sortValue) {
        return response -> response.getStatus() == BAD_REQUEST.getStatusCode()
                && response.getEntity().equals(String.format("Invalid sort parameter '%s'", sortValue));
    }
}
