/**
 * <h2>API usage</h2>
 *
 * Hibernate with Panache comes in two flavors, the active record pattern via
 * {@link io.quarkus.hibernate.reactive.panache.PanacheEntity}
 * and the repository pattern via {@link io.quarkus.hibernate.reactive.panache.PanacheRepository}.
 *
 * To use the active record pattern, make your entities extend {@link io.quarkus.hibernate.reactive.panache.PanacheEntity},
 * use public fields for your columns, use the existing operations defined as static methods on your entity class,
 * and define custom ones as static methods on your entity class:
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
 * To use the repository pattern, create a class implementing {@link io.quarkus.hibernate.reactive.panache.PanacheRepository},
 * use the existing operations from your repository and define new ones on your repository class:
 *
 * <code><pre>
 * &#64;ApplicationScoped
 * public class PersonRepository implements PanacheRepository&lt;Person&gt; {
 *    public Person findByName(String name){
 *        return find("name", name).firstResult();
 *    }
 *
 *    public List&lt;Person&gt; findAlive(){
 *        return list("status", Status.Alive);
 *    }
 *
 *    public void deleteStefs(){
 *        delete("name", "Stef");
 *   }
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
 * If your select query does not start with <code>from</code>, we support the following additional forms:
 * </p>
 * <ul>
 * <li><code>order by ...</code> which will expand to <code>from EntityName order by ...</code></li>
 * <li><code>&lt;singleColumnName&gt;</code> (and single parameter) which will expand to
 * <code>from EntityName where &lt;singleColumnName&gt; = ?</code></li>
 * <li><code>&lt;query&gt;</code> will expand to <code>from EntityName where &lt;query&gt;</code></li>
 * </ul>
 * 
 * If your update query does not start with <code>update from</code>, we support the following additional forms:
 * </p>
 * <ul>
 * <li><code>from EntityName ...</code> which will expand to <code>update from EntityName ...</code></li>
 * <li><code>set? &lt;singleColumnName&gt;</code> (and single parameter) which will expand to
 * <code>update from EntityName set &lt;singleColumnName&gt; = ?</code></li>
 * <li><code>set? &lt;update-query&gt;</code> will expand to
 * <code>update from EntityName set &lt;update-query&gt; = ?</code></li>
 * </ul>
 *
 * If your delete query does not start with <code>delete from</code>, we support the following additional forms:
 * </p>
 * <ul>
 * <li><code>from EntityName ...</code> which will expand to <code>delete from EntityName ...</code></li>
 * <li><code>&lt;singleColumnName&gt;</code> (and single parameter) which will expand to
 * <code>delete from EntityName where &lt;singleColumnName&gt; = ?</code></li>
 * <li><code>&lt;query&gt;</code> will expand to <code>delete from EntityName where &lt;query&gt;</code></li>
 * </ul>
 *
 * We also support named queries, for Panache to know that a query is a named query and not an HQL one, you need
 * to prefix the name of the query with '#'.
 * 
 * @author Stéphane Épardaud
 */
package io.quarkus.hibernate.reactive.panache;
