package io.quarkus.rest.client.reactive;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify a form parameter that should be sent with the outbound request.
 * When this annotation is placed at the interface level of a REST client interface, the specified form parameter will be sent
 * on each
 * request for all methods in the interface.
 * When this annotation is placed on a method, the parameter will be sent only for that method. If the same form parameter is
 * specified in an annotation for both the type and the method, only the parameter value specified in the annotation on the
 * method will be sent.
 * <p>
 * The value of the parameter to send can be specified explicitly by using the <code>value</code> attribute.
 * The value can also be computed via a default method on the client interface or a public static method on a different class.
 * The compute method must return a String or String[] (indicating a multivalued header) value. This method must be specified
 * in the <code>value</code> attribute but wrapped in curly-braces. The compute method's signature must either contain no
 * arguments or
 * a single <code>String</code> argument. The String argument is the name of the form parameter.
 * <p>
 * Here is an example that explicitly defines a form parameter value and computes a value:
 *
 * <pre>
 * public interface MyClient {
 *
 *    static AtomicInteger counter = new AtomicInteger(1);
 *
 *    default String determineFormParamValue(String name) {
 *        if ("SomeParam".equals(name)) {
 *            return "InvokedCount " + counter.getAndIncrement();
 *        }
 *        throw new UnsupportedOperationException("unknown name");
 *    }
 *
 *    {@literal @}ClientFormParam(name="SomeName", value="ExplicitlyDefinedValue")
 *    {@literal @}GET
 *    Response useExplicitFormParamValue();
 *
 *    {@literal @}ClientFormParam(name="SomeName", value="{determineFormParamValue}")
 *    {@literal @}DELETE
 *    Response useComputedFormParamValue();
 * }
 * </pre>
 *
 * The implementation should fail to deploy a client interface if the annotation contains a <code>@ClientFormParam</code>
 * annotation with a
 * <code>value</code> attribute that references a method that does not exist, or contains an invalid signature.
 * <p>
 * The <code>required</code> attribute will determine what action the implementation should take if the method specified in the
 * <code>value</code>
 * attribute throws an exception. If the attribute is true (default), then the implementation will abort the request and will
 * throw the exception
 * back to the caller. If the <code>required</code> attribute is set to false, then the implementation will not send this
 * form parameter if the method throws an exception.
 * <p>
 * Note that if an interface method contains an argument annotated with <code>@FormParam</code>, that argument will take
 * priority over anything specified in a <code>@ClientFormParam</code> annotation.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ClientFormParams.class)
public @interface ClientFormParam {

    /**
     * @return the name of the form param.
     */
    String name();

    /**
     * @return the value(s) of the param - or the method to invoke to get the value (surrounded by curly braces).
     */
    String[] value();

    /**
     * @return whether to abort the request if the method to compute the form parameter value throws an exception (true;
     *         default) or just
     *         skip this form parameter (false)
     */
    boolean required() default true;
}
