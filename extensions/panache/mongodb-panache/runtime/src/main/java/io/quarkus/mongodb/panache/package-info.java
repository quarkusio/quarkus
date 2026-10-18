/**
 * <h2>API usage</h2>
 *
 * MongoDB with Panache comes in two flavors, the active record pattern via
 * {@link io.quarkus.mongodb.panache.PanacheMongoEntity},
 * and the repository pattern via {@link io.quarkus.mongodb.panache.PanacheMongoRepository}.
 *
 * To use the active record pattern, make your entities extend {@link io.quarkus.mongodb.panache.PanacheMongoEntity},
 * use public fields for your columns, use the existing operations defined as static methods on your entity class,
 * and define custom ones as static methods on your entity class:
 *
 * <code><pre>
 * public class Person extends PanacheMongoEntity {
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
 * To use the repository pattern, create a class implementing {@link io.quarkus.mongodb.panache.PanacheMongoRepository},
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
 * You can also use {@link io.quarkus.mongodb.panache.PanacheMongoRepository} if you prefer the repository approach.
 *
 * <p>
 * You can use the <code>@MongoEntity</code> annotation to define the name of the MongoDB collection,
 * otherwise it will be the name of your entity.
 * </p>
 * <p>
 * The Mongo PojoCodec is used to serialize your entity to a Bson Document, you can find more information on its
 * documentation page:
 * https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/document-data-format-pojo.
 * This codec also supports Java records.
 * You can use the MongoDB annotations to control the mapping to the database : <code>@BsonId</code>,
 * <code>@BsonProperty("fieldName")</code>, <code>@BsonIgnore</code>.
 * </p>
 *
 * <h2>Simplified queries</h2>
 *
 * <p>
 * Normally, MongoDB queries are of this form: <code>{"field1": "value1", "field2": "value2"}</code>
 * </p>
 * <p>
 * We support multiple convenience query implementations, this is what we called PanacheQL queries:
 * <ul>
 * <li>You can use one of the three flavours or parameterized query:
 * <ul>
 * <li>find("field", value)</li>
 * <li>find("field = ?1", value)</li>
 * <li>find("field = :value", Parameters.with("value", value)</li>
 * </ul>
 * They will all generates the same query : {"field": "value"}.
 * </li>
 * <li>We support the following query operators: 'and', 'or' ( mixing 'and' and 'or' is not currently supported), '=',
 * '>', '>=', '<', '<=', '!=', 'is null', 'is not null', 'in' and 'like' that is mapped to the MongoDB `$regex` operator
 * (both String and JavaScript patterns are supported).</li>
 * <li>field replacement is supported based on the value of the <code>@BsonProperty</code> annotations</li>
 * </ul>
 * </p>
 * <p>
 * <b>WARNING</b>: MongoDB queries must be valid JSON documents,
 * using the same field multiple times in a query is not allowed using PanacheQL as it would generate an invalid JSON
 * (see this issue on github: https://github.com/quarkusio/quarkus/issues/12086).
 * </p>
 * <p>
 * You can also write native MongoDB queries, in this case the field names are not replaced even if you use
 * <code>@BsonProperty</code>, but you can still use parameterized queries by index or name.<br/>
 * <code>find("{'field':?1}", value)</code> or <code>find("{'field'::key}", value)</code>
 * </p>
 *
 * @author Loïc Mathieu
 */
package io.quarkus.mongodb.panache;
