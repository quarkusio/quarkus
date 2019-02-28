/**
 * <h2>API usage</h2>
 * 
 * Make your entities extend {@link io.quarkus.panache.jpa.PanacheEntity}, use public fields for your columns, use the existing
 * operations defined as static methods on your entity class, and define custom ones as static methods on your entity class:
 * 
 * <code><pre>
 * &#64;Entity
 * public class Person extends PanacheEntity {
 *     public String name;
 *     public LocalDate birth;
 *     public PersonStatus status;
 *     
 *     public static Person findByName(String name){
 *       return find("name", name).firstResult();
 *     }
 *     
 *     public static List&lt;Person&gt; findAlive(){
 *       return list("status", Status.Alive);
 *     }
 *     
 *     public static void deleteStefs(){
 *       delete("name", "Stef");
 *     }
 * }
 * </pre></code>
 * 
 * <h2>Simplified queries</h2>
 * 
 * <p>
 * Normally, HQL queries are of this form: <code>from EntityName [where ...] [order by ...]</code>, with optional elements
 * at the end.
 * </p>
 * <p>
 * If your query does not start with <code>from</code>, we support the following additional forms:
 * </p>
 * <ul>
 * <li><code>order by ...</code> which will expand to <code>from EntityName order by ...</code></li>
 * <li><code>&lt;singleColumnName&gt;</code> (and single parameter) which will expand to
 * <code>from EntityName where &lt;singleColumnName&gt; = ?</code></li>
 * <li><code>&lt;query&gt;</code> will expand to <code>from EntityName where &lt;query&gt;</code></li>
 * </ul>
 * 
 * @author Stéphane Épardaud
 */
package io.quarkus.panache.jpa;
