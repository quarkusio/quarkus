package io.quarkus.bootstrap.json;

public sealed interface JsonValue permits JsonBoolean, JsonMember, JsonMultiValue, JsonNull, JsonNumber, JsonString {

    /**
     * Unwraps this JSON value to its Java equivalent.
     * <ul>
     * <li>{@link JsonString} &rarr; {@link String}</li>
     * <li>{@link JsonInteger} &rarr; {@link Integer} (if the value fits) or {@link Long}</li>
     * <li>{@link JsonDouble} &rarr; {@link Double}</li>
     * <li>{@link JsonBoolean} &rarr; {@link Boolean}</li>
     * <li>{@link JsonNull} &rarr; {@code null}</li>
     * <li>{@link JsonObject} &rarr; {@link java.util.Map Map&lt;String, Object&gt;} (via {@link JsonObject#toMap()})</li>
     * <li>{@link JsonArray} &rarr; {@link java.util.List List&lt;Object&gt;} (via {@link JsonArray#toList()})</li>
     * <li>{@link JsonMember} &rarr; unwraps the member's value</li>
     * </ul>
     *
     * @return the Java equivalent of this JSON value
     */
    default Object unwrap() {
        if (this instanceof JsonString s) {
            return s.value();
        }
        if (this instanceof JsonInteger n) {
            long l = n.longValue();
            if (l == (int) l) {
                return (int) l;
            }
            return l;
        }
        if (this instanceof JsonDouble d) {
            return d.value();
        }
        if (this instanceof JsonBoolean b) {
            return b.value();
        }
        if (this instanceof JsonNull) {
            return null;
        }
        if (this instanceof JsonObject o) {
            return o.toMap();
        }
        if (this instanceof JsonArray a) {
            return a.toList();
        }
        if (this instanceof JsonMember m) {
            return m.value().unwrap();
        }
        return toString();
    }
}
