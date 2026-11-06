package io.quarkus.rest.data.panache.runtime.sort;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.OptionalInt;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.quarkus.rest.data.panache.runtime.RestDataPanacheConfig;
import io.quarkus.security.identity.SecurityIdentity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SortQueryParamFilterTest {

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private ResourceInfo resourceInfo;

    @Mock
    private Instance<SecurityIdentity> securityIdentityInstance;

    @Mock
    private SecurityIdentity securityIdentity;

    @Mock
    private Instance<RestDataPanacheConfig> configInstance;

    @Mock
    private RestDataPanacheConfig config;

    @InjectMocks
    private SortQueryParamFilter filter;

    @BeforeEach
    void setUp() {
        given(requestContext.getUriInfo()).willReturn(uriInfo);
        given(configInstance.isResolvable()).willReturn(true);
        given(configInstance.get()).willReturn(config);
        given(config.maxPageSize()).willReturn(OptionalInt.of(100));
    }

    @Test
    void shouldAllowValidParameters() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "$name");
        map.putSingle("sort", "-surname_1");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithFieldsIncluding("$name", "surname_1"));

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldAllowValidParametersWithMultipleValues() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "$name,-surname_1");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithFieldsIncluding("$name", "surname_1"));

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldAllowEmptyParameters() throws NoSuchMethodException {
        given(uriInfo.getQueryParameters()).willReturn(new MultivaluedHashMap<>());
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldCatchInvalidParameter() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "$name");
        map.putSingle("sort", "(surname_1");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithFieldsIncluding("$name"));

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Invalid sort parameter '(surname_1'")));
    }

    @Test
    void shouldCatchInvalidParameterValue() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "$name,1_surname");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithFieldsIncluding("$name"));

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Invalid sort parameter '$name,1_surname'")));
    }

    @Test
    void shouldAllowFieldInWhitelist() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "name");
        map.putSingle("sort", "-age");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldRejectFieldNotInWhitelist() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "unknownField");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Unknown sort field 'unknownField'")));
    }

    @Test
    void shouldRejectFieldNotInWhitelistWithDescending() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "-secret");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Unknown sort field 'secret'")));
    }

    @Test
    void shouldRejectAllFieldsWhenWhitelistIsEmpty() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "anyField");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithEmptyAllowedFields());

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Unknown sort field 'anyField'")));
    }

    @Test
    void shouldRejectNestedProperties() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "author.name");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Invalid sort parameter 'author.name'")));
    }

    @Test
    void shouldAllowSecureFieldWithValidRole() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "salary");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());
        given(securityIdentityInstance.isResolvable()).willReturn(true);
        given(securityIdentityInstance.get()).willReturn(securityIdentity);
        given(securityIdentity.isAnonymous()).willReturn(false);
        given(securityIdentity.hasRole("admin")).willReturn(true);

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldRejectSecureFieldWhenSecurityNotAvailable() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "salary");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());
        given(securityIdentityInstance.isResolvable()).willReturn(false);

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Invalid sort field 'salary'")));
    }

    @Test
    void shouldRejectSecureFieldWhenUserIsAnonymous() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "salary");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());
        given(securityIdentityInstance.isResolvable()).willReturn(true);
        given(securityIdentityInstance.get()).willReturn(securityIdentity);
        given(securityIdentity.isAnonymous()).willReturn(true);

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Invalid sort field 'salary'")));
    }

    @Test
    void shouldRejectSecureFieldWhenUserLacksRole() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "salary");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());
        given(securityIdentityInstance.isResolvable()).willReturn(true);
        given(securityIdentityInstance.get()).willReturn(securityIdentity);
        given(securityIdentity.isAnonymous()).willReturn(false);
        given(securityIdentity.hasRole("admin")).willReturn(false);
        given(securityIdentity.hasRole("hr")).willReturn(false);

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Invalid sort field 'salary'")));
    }

    @Test
    void shouldAllowSecureFieldWithAnyMatchingRole() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "salary");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());
        given(securityIdentityInstance.isResolvable()).willReturn(true);
        given(securityIdentityInstance.get()).willReturn(securityIdentity);
        given(securityIdentity.isAnonymous()).willReturn(false);
        given(securityIdentity.hasRole("admin")).willReturn(false);
        given(securityIdentity.hasRole("hr")).willReturn(true);

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldAllowNonSecureFieldWithoutAuthentication() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("sort", "name");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    // Filter param tests

    @Test
    void shouldAllowFilterBySecureFieldWithValidRole() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("salary", "50000");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());
        given(securityIdentityInstance.isResolvable()).willReturn(true);
        given(securityIdentityInstance.get()).willReturn(securityIdentity);
        given(securityIdentity.isAnonymous()).willReturn(false);
        given(securityIdentity.hasRole("admin")).willReturn(true);

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldRejectFilterBySecureFieldWhenSecurityNotAvailable() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("salary", "50000");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());
        given(securityIdentityInstance.isResolvable()).willReturn(false);

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Invalid filter field 'salary'")));
    }

    @Test
    void shouldRejectFilterBySecureFieldWhenUserLacksRole() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("salary", "50000");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());
        given(securityIdentityInstance.isResolvable()).willReturn(true);
        given(securityIdentityInstance.get()).willReturn(securityIdentity);
        given(securityIdentity.isAnonymous()).willReturn(false);
        given(securityIdentity.hasRole("admin")).willReturn(false);
        given(securityIdentity.hasRole("hr")).willReturn(false);

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Invalid filter field 'salary'")));
    }

    @Test
    void shouldAllowFilterByNonSecureFieldWithoutAuthentication() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("name", "John");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldRejectFilterByUnknownField() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("unknownField", "value");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Unknown filter field 'unknownField'")));
    }

    @Test
    void shouldAllowFilterByAllowedField() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("name", "John");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldIgnoreReservedParams() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("page", "1");
        map.putSingle("size", "20");
        map.putSingle("namedQuery", "findByName");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithSecureFields());

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    // Page size validation tests

    @Test
    void shouldAllowPageSizeWithinLimit() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("size", "100");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @Test
    void shouldRejectPageSizeExceedingLimit() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("size", "200");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(BAD_REQUEST.getStatusCode(),
                "Page size 200 exceeds maximum allowed size of 100")));
    }

    @Test
    void shouldReturnInternalErrorWhenConfigNotResolvable() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("size", "10");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());
        given(configInstance.isResolvable()).willReturn(false);

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal server error")));
    }

    @Test
    void shouldReturnInternalErrorWhenMaxPageSizeNotConfigured() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("size", "10");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());
        given(config.maxPageSize()).willReturn(OptionalInt.empty());

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(abortResponseMatcher(INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal server error")));
    }

    @Test
    void shouldIgnoreInvalidSizeFormatForPageSizeValidation() throws NoSuchMethodException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("size", "notanumber");
        given(uriInfo.getQueryParameters()).willReturn(map);
        given(resourceInfo.getResourceMethod()).willReturn(getMethodWithAllowedFields());

        filter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    private ArgumentMatcher<Response> abortResponseMatcher(int status, String message) {
        return response -> response.getStatus() == status
                && response.getEntity().equals(message);
    }

    private Method getMethodWithAllowedFields() throws NoSuchMethodException {
        return TestResource.class.getMethod("listWithAllowedFields");
    }

    private Method getMethodWithFieldsIncluding(String... fields) throws NoSuchMethodException {
        return FieldsIncludingResource.getMethod(fields);
    }

    private Method getMethodWithEmptyAllowedFields() throws NoSuchMethodException {
        return TestResource.class.getMethod("listWithEmptyAllowedFields");
    }

    private Method getMethodWithSecureFields() throws NoSuchMethodException {
        return TestResource.class.getMethod("listWithSecureFields");
    }

    static class TestResource {
        @SortQueryParamValidator(allowedFields = { "name", "age", "email" })
        public void listWithAllowedFields() {
        }

        @SortQueryParamValidator
        public void listWithEmptyAllowedFields() {
        }

        @SortQueryParamValidator(allowedFields = { "name", "age", "salary" }, secureFields = { "salary:admin,hr" })
        public void listWithSecureFields() {
        }
    }

    static class FieldsIncludingResource {
        @SortQueryParamValidator(allowedFields = { "$name", "surname_1" })
        public void listWithDollarAndUnderscore() {
        }

        static Method getMethod(String... fields) throws NoSuchMethodException {
            return FieldsIncludingResource.class.getMethod("listWithDollarAndUnderscore");
        }
    }
}
