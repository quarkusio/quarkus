/**
 * <h2>API usage</h2>
 * FIXME: REWRITE
 *
 * <h2>Simplified queries</h2>
 *
 * <p>
 * Normally, HQL queries are of this form: <code>from EntityName [where ...] [order by ...]</code>, with optional elements
 * at the end.
 * </p>
 * <p>
 * If your select query does not start with <code>from</code>, <code>select</code>, or <code>with</code>, we support the
 * following additional forms:
 * </p>
 * <ul>
 * <li><code>order by ...</code> which will expand to <code>from EntityName order by ...</code></li>
 * <li><code>&lt;singleAttribute&gt;</code> (and single parameter) which will expand to
 * <code>from EntityName where &lt;singleAttribute&gt; = ?</code></li>
 * <li><code>where &lt;query&gt;</code> will expand to <code>from EntityName where &lt;query&gt;</code>
 * <li><code>&lt;query&gt;</code> will expand to <code>from EntityName where &lt;query&gt;</code></li>
 * </ul>
 *
 * <p>
 * If your update query does not start with <code>update from</code>, we support the following additional forms:
 * </p>
 * <ul>
 * <li><code>from EntityName ...</code> which will expand to <code>update from EntityName ...</code></li>
 * <li><code>set? &lt;singleAttribute&gt;</code> (and single parameter) which will expand to
 * <code>update from EntityName set &lt;singleAttribute&gt; = ?</code></li>
 * <li><code>set? &lt;update-query&gt;</code> will expand to
 * <code>update from EntityName set &lt;update-query&gt; = ?</code></li>
 * </ul>
 *
 * <p>
 * If your delete query does not start with <code>delete from</code>, we support the following additional forms:
 * </p>
 * <ul>
 * <li><code>from EntityName ...</code> which will expand to <code>delete from EntityName ...</code></li>
 * <li><code>&lt;singleAttribute&gt;</code> (and single parameter) which will expand to
 * <code>delete from EntityName where &lt;singleAttribute&gt; = ?</code></li>
 * <li><code>&lt;query&gt;</code> will expand to <code>delete from EntityName where &lt;query&gt;</code></li>
 * </ul>
 *
 * We also support named queries, for Panache to know that a query is a named query and not an HQL one, you need
 * to prefix the name of the query with '#'.
 *
 * @author Stéphane Épardaud
 */
package io.quarkus.data.hibernate;
