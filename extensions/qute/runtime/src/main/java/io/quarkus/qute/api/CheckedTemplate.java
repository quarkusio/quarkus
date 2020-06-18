package io.quarkus.qute.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.qute.TemplateInstance;

/**
 * <p>
 * If you place this annotation on a class, all its <code>native static</code> methods will be used to declare
 * templates and the list of parameters they require.
 * <p>
 * If this is placed on an inner class of the class <code>X</code>, a <code>native static</code> method of the name
 * <code>foo</code> will refer to a template at the path <code>X/foo</code> (template file extensions are
 * not part of the method name) relative to your templates root.
 * <p>
 * If this is placed on a toplevel class, a <code>native static</code> method of the name
 * <code>foo</code> will refer to a template at the path <code>foo</code> (template file extensions are
 * not part of the method name) at the toplevel of your templates root.
 * <p>
 * Each parameter of the <code>native static</code> will be used to validate the template at build time, to
 * make sure that those parameters are used properly in a type-safe manner. The return type of each
 * <code>native static</code> method should be {@link TemplateInstance}.
 * <p>
 * Example:
 * <p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Path("item")
 *     public class ItemResource {
 * 
 *         &#64;CheckedTemplate
 *         class Templates {
 *             // defines a template at ItemResource/item, taking an Item parameter named item
 *             public static native TemplateInstance item(Item item);
 *         }
 * 
 *         &#64;GET
 *         &#64;Path("{id}")
 *         &#64;Produces(MediaType.TEXT_HTML)
 *         public TemplateInstance get(@PathParam("id") Integer id) {
 *             // instantiate that template and pass it the required template parameter
 *             return Templates.item(service.findItem(id));
 *         }
 *     }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckedTemplate {

}
