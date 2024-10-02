package io.quarkus.qute;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>
 * <strong>IMPORTANT: This annotation only works in a fully integrated environment; such as a Quarkus application.</strong>
 * </p>
 *
 * Enables registration of additional components to the preconfigured {@link Engine}.
 * <p>
 * A non-abstract, top-level or static nested class that implements one of the <b>supported component interfaces</b> and
 * is annotated
 * with this annotation:
 * <ul>
 * <li>can be used during validation of templates at build time,</li>
 * <li>is automatically registered at runtime (a) to the preconfigured {@link Engine} and (b) as a CDI bean.</li>
 * </ul>
 *
 * The list of supported component interfaces includes: {@link SectionHelperFactory}, {@link ValueResolver},
 * {@link NamespaceResolver} and {@link ParserHook}.
 * <p>
 * An annotated class that implements {@link SectionHelperFactory} or {@link ParserHook} must be public and declare a no-args
 * constructor that is used to instantiate the component at build time. At runtime, a CDI bean instance is used. This means that
 * the factory can declare injection points. However, these injection points are only injected at runtime. If no CDI scope is
 * defined then {@code javax.enterprise.context.Dependent} is used.
 *
 * @see EngineBuilder#addSectionHelper(SectionHelperFactory)
 * @see EngineBuilder#addValueResolver(ValueResolver)
 * @see EngineBuilder#addNamespaceResolver(NamespaceResolver)
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
public @interface EngineConfiguration {

}
