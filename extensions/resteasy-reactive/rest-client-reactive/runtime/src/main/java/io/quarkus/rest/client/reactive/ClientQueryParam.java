package io.quarkus.rest.client.reactive;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify a query that should be sent with the outbound request.
 * When this annotation is placed at the interface level of a REST client interface, the specified query will be sent on each
 * request for all
 * methods in the interface.
 * When this annotation is placed on a method, the parameter will be sent only for that method. If the same query parameter is
 * specified in an annotation
 * for both the type and the method, only the parameter value specified in the annotation on the method will be sent.
 * <p>
 * The value of the parameter to send can be specified explicitly by using the <code>value</code> attribute.
 * The value can also be computed via a default method on the client interface or a public static method on a different class.
 * The compute method
 * must return a String or String[] (indicating a multivalued query) value. This method must be specified in the
 * <code>value</code> attribute but
 * wrapped in curly-braces. The compute method's signature must either contain no arguments or a single <code>String</code>
 * argument. The String argument is the name of the query.
 * <p>
 * Here is an example that explicitly defines a query value and computes a value:
 *
 * <pre>
 * public interface MyClient {
 *
 *    static AtomicInteger counter = new AtomicInteger(1);
 *
 *    default String determineQueryValue(String name) {
 *        if ("SomeQuery".equals(name)) {
 *            return "InvokedCount " + counter.getAndIncrement();
 *        }
 *        throw new UnsupportedOperationException("unknown name");
 *    }
 *
 *    {@literal @}ClientQueryParam(name="SomeName", value="ExplicitlyDefinedValue")
 *    {@literal @}GET
 *    Response useExplicitQueryValue();
 *
 *    {@literal @}ClientQueryParam(name="SomeName", value="{determineQueryValue}")
 *    {@literal @}DELETE
 *    Response useComputedQueryValue();
 * }
 * </pre>
 *
 * The implementation should fail to deploy a client interface if the annotation contains a <code>@ClientQueryParam</code>
 * annotation with a
 * <code>value</code> attribute that references a method that does not exist, or contains an invalid signature.
 * <p>
 * The <code>required</code> attribute will determine what action the implementation should take if the method specified in the
 * <code>value</code>
 * attribute throws an exception. If the attribute is true (default), then the implementation will abort the request and will
 * throw the exception
 * back to the caller. If the <code>required</code> attribute is set to false, then the implementation will not send this query
 * if the method throws
 * an exception.
 * <p>
 * Note that if an interface method contains an argument annotated with <code>@QueryParam</code>, that argument will take
 * priority over anything
 * specified in a <code>@ClientQueryParam</code> annotation.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ClientQueryParams.class)
public @interface ClientQueryParam {

    /**
     * @return the name of the query param.
     */
    String name();

    /**
     * @return the value(s) of the param - or the method to invoke to get the value (surrounded by curly braces).
     */
    String[] value();

    /**
     * @return whether to abort the request if the method to compute the query value throws an exception (true; default) or just
     *         skip this query
     *         (false)
     */
    boolean required() default true;
}
