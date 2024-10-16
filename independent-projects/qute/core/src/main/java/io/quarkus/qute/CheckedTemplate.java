package io.quarkus.qute;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * <strong>IMPORTANT: This annotation only works in a fully integrated environment; such as a Quarkus application.</strong>
 * </p>
 *
 * It aims to configure type-safe templates.
 * You can annotate a class or a Java record that implements {@link TemplateInstance}.
 *
 * <h2>&#64;CheckedTemplate on a class</h2>
 * If you place this annotation on a class, then all its <code>static native</code> methods will be used to declare
 * templates and the list of parameters they require.
 * <p>
 * The name of a method and the base path are used to locate the template contents.
 * By default, the base path is derived from the annotation target:
 * <ul>
 * <li>If this is placed on a static nested class of an enclosing class with a simple name <code>X</code>, a
 * <code>native static</code> method of the name <code>foo</code> will refer to a template at the path <code>X/foo</code>
 * (template file extensions are
 * not part of the method name) relative to the templates root.</li>
 * <li>If this is placed on a top-level class, a <code>native static</code> method of the name <code>foo</code> will refer to a
 * template at the path <code>foo</code> (template file extensions are
 * not part of the method name) at the toplevel of the templates root.</li>
 * </ul>
 * The base path can be also specified via {@link #basePath()}.
 * <p>
 * Each parameter of the <code>native static</code> will be used to validate the template at build time, to
 * make sure that those parameters are used properly in a type-safe manner. The return type of each
 * <code>native static</code> method should be {@link TemplateInstance}.
 * <p>
 * Example:
 * <p>
 *
 * <pre>
 * &#64;Path("item")
 * public class ItemResource {
 *
 *     &#64;CheckedTemplate
 *     static class Templates {
 *         // defines a template at ItemResource/item, taking an Item parameter named item
 *         static native TemplateInstance item(Item item);
 *     }
 *
 *     &#64;GET
 *     &#64;Path("{id}")
 *     &#64;Produces(MediaType.TEXT_HTML)
 *     public TemplateInstance get(@PathParam("id") Integer id) {
 *         // instantiate that template and pass it the required template parameter
 *         return Templates.item(service.findItem(id));
 *     }
 * }
 * </pre>
 *
 * <h2>&#64;CheckedTemplate on a template record</h2>
 *
 * If you place this annotation on a Java record that implements {@link TemplateInstance}, then attributes of this annotation
 * are used to configure the non-default values of the type-safe template denoted by this record.
 *
 * <h2>Type-safe fragments</h2>
 *
 * By default, a <code>native static</code> method or a template record with the name that contains a dollar sign {@code $}
 * denotes a fragment of a type-safe template.
 * It's possible to ignore the fragments and effectively disable this feature via {@link CheckedTemplate#ignoreFragments()}.
 * <p>
 * The name of the fragment is derived from the annotated element name. The part before the last occurence of a dollar sign
 * {@code $} is the method name of the related type-safe template. The part after the last occurence of a dollar sign is the
 * fragment identifier - the strategy defined by the relevant {@link CheckedTemplate#defaultName()} is used.
 * <p>
 * Parameters of the annotated element are validated. The required names and types are derived from the relevant fragment
 * template.
 *
 * <pre>
 * &#64;CheckedTemplate
 * class Templates {
 *
 *     // defines a type-safe template
 *     static native TemplateInstance items(List&#60;Item&#62; items);
 *
 *     // defines a fragment of Templates#items() with identifier "item"
 *     &#64;CheckedFragment
 *     static native TemplateInstance items$item(Item item);
 * }
 * </pre>
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckedTemplate {

    /**
     * Constant value for {@link #basePath()} indicating that the default strategy should be used, i.e. the simple name of the
     * enclosing class for a nested static class or an empty string for a top level class.
     */
    String DEFAULTED = "<<defaulted>>";

    /**
     * Constant value for {@link #defaultName()} indicating that the method name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * Constant value for {@link #defaultName()} indicating that the annotated element's name should be de-camel-cased and
     * hyphenated, and then used.
     */
    String HYPHENATED_ELEMENT_NAME = "<<hyphenated element name>>";

    /**
     * Constant value for{@link #defaultName()} indicating that the annotated element's name should be de-camel-cased and parts
     * separated by underscores, and then used.
     */
    String UNDERSCORED_ELEMENT_NAME = "<<underscored element name>>";

    /**
     * Example:
     *
     * <pre>
     * &#64;Path("item")
     * public class ItemResource {
     *
     *     &#64;CheckedTemplate(basePath = "items_v1")
     *     static class Templates {
     *         // defines a template at items_v1/item
     *         static native TemplateInstance item(Item item);
     *
     *         // defines a template at items_v1/allItems
     *         static native TemplateInstance allItems(List&lt;Item&gt; items);
     *     }
     * }
     * </pre>
     *
     * @return the base path relative to the templates root
     */
    String basePath() default DEFAULTED;

    /**
     * If set to true then the defined templates can only contain type-safe expressions.
     */
    boolean requireTypeSafeExpressions() default true;

    /**
     * The value may be one of the following: {@link #ELEMENT_NAME}, {@link #HYPHENATED_ELEMENT_NAME} and
     * {@link #UNDERSCORED_ELEMENT_NAME}.
     *
     * @return the default name
     */
    String defaultName() default ELEMENT_NAME;

    /**
     * By default, a <code>native static</code> method with the name that contains a dollar sign {@code $} denotes a method that
     * represents a fragment of a type-safe template. It's possible to ignore the fragments and effectively disable this
     * feature.
     *
     * @return {@code true} if no method should be interpreted as a fragment, {@code false} otherwise
     * @see Template#getFragment(String)
     */
    boolean ignoreFragments() default false;

}
