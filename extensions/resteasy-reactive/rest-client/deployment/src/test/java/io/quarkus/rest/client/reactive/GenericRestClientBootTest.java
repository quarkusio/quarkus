package io.quarkus.rest.client.reactive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that generic REST client interfaces can boot successfully with Hibernate Validator.
 * <p>
 * Regression test for https://github.com/quarkusio/quarkus/issues/57838
 * <p>
 * The bug: When a @RegisterRestClient interface declares type parameters, the generated CDIWrapper
 * has methods with generic signatures referencing type variables the wrapper class doesn't declare.
 * This causes NullPointerException in ClassMate when Hibernate Validator initializes.
 * <p>
 * The fix: Use erased signatures for wrapper methods when the interface has type parameters.
 */
public class GenericRestClientBootTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            // Test Case 1: Single type parameter
                            GenericClient.class, BaseClient.class, Dto.class, Wrapper.class,
                            // Test Case 2: Multiple type parameters
                            MultiGenericClient.class, Request.class, Response.class,
                            // Test Case 3: Bounded type parameter
                            BoundedGenericClient.class, BaseDto.class, ExtendedDto.class,
                            // Test Case 4: Generic hierarchy
                            GenericRepository.class, CrudClient.class, Entity.class,
                            // Test Case 5: Non-generic (backward compatibility)
                            SimpleClient.class))
            .withConfigurationResource("generic-client-test-application.properties");

    @Test
    public void testApplicationBootsWithGenericClients() {
        // This is a boot test - the test passes if the application starts successfully.
        //
        // Before fix, boot failed with:
        // java.lang.NullPointerException: Cannot invoke "Object.getClass()" because "mainType" is null
        //   at com.fasterxml.classmate.TypeResolver._fromAny(TypeResolver.java:351)
        //
        // The NPE occurred during Hibernate Validator initialization when processing CDI wrappers
        // for generic REST client interfaces. The wrappers had methods with generic signatures
        // referencing type variables that the wrapper class didn't declare.
        //
        // After fix: CDI wrappers use erased signatures, preventing the NPE.
        // If we reach this point, boot succeeded and the bug is fixed.
    }

    // ==================== Test Data Classes ====================

    public static class Dto {
        public String name;
    }

    public static class Wrapper<T> {
        public T data;
    }

    public static class Request<T> {
        public T payload;
    }

    public static class Response<T> {
        public T result;
    }

    public static class BaseDto {
        public Long id;
    }

    public static class ExtendedDto extends BaseDto {
        public String extra;
    }

    public static class Entity {
        public Long id;
    }

    // ==================== Test Client Interfaces ====================

    /**
     * Test Case 1: Single type parameter with default method
     */
    public interface BaseClient<T> {
        @GET
        @Path("/{id}")
        default Wrapper<T> get(@PathParam("id") Long id) {
            return null;
        }
    }

    @Path("/generic")
    @RegisterRestClient(configKey = "generic")
    public interface GenericClient<T extends Dto> extends BaseClient<T> {
        @POST
        Wrapper<String> create(Wrapper<T> body);
    }

    /**
     * Test Case 2: Multiple type parameters
     */
    @Path("/multi")
    @RegisterRestClient(configKey = "multi")
    public interface MultiGenericClient<REQ, RESP> {
        @POST
        Response<RESP> process(Request<REQ> request);
    }

    /**
     * Test Case 3: Bounded type parameter
     */
    @Path("/bounded")
    @RegisterRestClient(configKey = "bounded")
    public interface BoundedGenericClient<T extends BaseDto> {
        @POST
        T save(T entity);
    }

    /**
     * Test Case 4: Generic hierarchy
     */
    public interface GenericRepository<E> {
        @POST
        @Path("/save")
        E save(E entity);
    }

    @Path("/crud")
    @RegisterRestClient(configKey = "crud")
    public interface CrudClient<E extends Entity> extends GenericRepository<E> {
        @POST
        @Path("/delete/{id}")
        void delete(@PathParam("id") Long id);
    }

    /**
     * Test Case 5: Non-generic client (backward compatibility)
     */
    @Path("/simple")
    @RegisterRestClient(configKey = "simple")
    public interface SimpleClient {
        @POST
        Wrapper<String> create(Wrapper<Dto> body);
    }
}
