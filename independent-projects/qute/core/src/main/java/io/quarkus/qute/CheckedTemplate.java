package io.quarkus.qute;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If you place this annotation on a class, all its <code>native static</code> methods will be used to declare
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
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckedTemplate {

    /**
     * Constant value for {@link #basePath()} indicating that the default strategy should be used.
     */
    String DEFAULTED = "<<defaulted>>";

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

}
