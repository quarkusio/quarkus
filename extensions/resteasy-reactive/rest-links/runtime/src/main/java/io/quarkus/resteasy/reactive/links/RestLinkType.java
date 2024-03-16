package io.quarkus.resteasy.reactive.links;

/**
 * Manage the link types to be injected in the Web links.
 */
public enum RestLinkType {
    /**
     * It will inject the links that return the link type {@link RestLink#entityType()} without filtering or searching.
     * For example:
     *
     * <pre>
     *     &#64;GET
     *     &#64;Path("/records")
     *     &#64;RestLink(rel = "list")
     *     &#64;InjectRestLinks(RestLinkType.TYPE)
     *     public List<Record> getAll() { // ... }
     *
     *     &#64;GET
     *     &#64;Path("/records/valid")
     *     &#64;RestLink
     *     public List<Record> getValidRecords() { // ... }
     *
     *     &#64;GET
     *     &#64;Path("/records/{id}")
     *     &#64;RestLink(rel = "self")
     *     public TestRecord getById(@PathParam("id") int id) { // ... }
     * </pre>
     * <p>
     * Note that the method `getAll` is annotated with `@InjectRestLinks(RestLinkType.TYPE)`, so when calling to the endpoint
     * `/records`, it will inject the following links:
     *
     * <pre>
     * Link: <http://localhost:8080/records>; rel="list"
     * Link: <http://localhost:8080/records/valid>; rel="getValidRecords"
     * </pre>
     * <p>
     * The method `getById` is not injected because it's instance based (it depends on the field `id`).
     */
    TYPE,

    /**
     * It will inject all the links that return the link type {@link RestLink#entityType()}.
     * For example:
     *
     * <pre>
     *
     *     &#64;GET
     *     &#64;RestLink(rel = "list")
     *     public List<TestRecord> getAll() { // ... }
     *
     *     &#64;GET
     *     &#64;Path("/records/{id}")
     *     &#64;RestLink(rel = "self")
     *     &#64;InjectRestLinks(RestLinkType.INSTANCE)
     *     public TestRecord getById(@PathParam("id") int id) { // ... }
     *
     *     &#64;GET
     *     &#64;Path("/records/{slug}")
     *     &#64;RestLink
     *     public TestRecord getBySlug(@PathParam("slug") String slug) { // ... }
     *
     *     &#64;DELETE
     *     &#64;Path("/records/{id}")
     *     &#64;RestLink
     *     public TestRecord delete(@PathParam("slug") String slug) { // ... }
     * </pre>
     * <p>
     * Note that the method `getById` is annotated with `@InjectRestLinks(RestLinkType.INSTANCE)`, so when calling to the
     * endpoint `/records/1`, it will inject the following links:
     *
     * <pre>
     * Link: <http://localhost:8080/records>; rel="list"
     * Link: <http://localhost:8080/records/1>; rel="self"
     * Link: <http://localhost:8080/records/theSlugValueInRecord1>; rel="getBySlug"
     * Link: <http://localhost:8080/records/1>; rel="delete"
     * </pre>
     * <p>
     * Now, all the links have been injected.
     */
    INSTANCE
}
