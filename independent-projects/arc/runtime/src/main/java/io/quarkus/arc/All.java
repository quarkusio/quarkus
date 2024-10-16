package io.quarkus.arc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * The container provides a synthetic bean for an injection point with the required type {@link List} and the required qualifier
 * {@link All}. The injected instance is an immutable list of the contextual references of the disambiguated beans.
 *
 * <pre>
 * &#064;ApplicationScoped
 * public class Processor {
 *
 *     &#064;Inject
 *     &#064;All
 *     List&lt;Service&gt; services;
 * }
 * </pre>
 *
 * If the injection point declares no other qualifier then {@link Any} is used, i.e. the behavior is equivalent to
 * {@code @Inject @Any Instance<Service> services}. The semantics is the same as for the {@link Instance#iterator()}, i.e. the
 * container attempts to resolve ambiguities. In general, if multiple beans are eligible then the container eliminates all beans
 * that are:
 * <ul>
 * <li>not alternatives, except for producer methods and fields of beans that are alternatives,</li>
 * <li>default beans.</li>
 * </ul>
 *
 * You can also inject a list of bean instances wrapped in {@link InstanceHandle}. This can be useful if you need to inspect the
 * bean metadata.
 *
 * <pre>
 * &#064;ApplicationScoped
 * public class Processor {
 *
 *     &#064;Inject
 *     &#064;All
 *     List&lt;InstanceHandle&lt;Service&gt;&gt; services;
 *
 *     void doSomething() {
 *         for (InstanceHandle&lt;Service&gt; handle : services) {
 *             if (handle.getBean().getScope().equals(Dependent.class)) {
 *                 handle.get().process();
 *                 break;
 *             }
 *         }
 *     }
 * }
 * </pre>
 *
 * The list is sorted by {@link InjectableBean#getPriority()}. Higher priority goes first.
 *
 * @see jakarta.annotation.Priority
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, PARAMETER })
public @interface All {

    /**
     * Supports inline instantiation of this qualifier.
     */
    public static final class Literal extends AnnotationLiteral<All> implements All {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

    }

}
