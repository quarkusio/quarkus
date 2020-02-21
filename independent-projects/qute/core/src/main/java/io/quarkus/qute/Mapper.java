package io.quarkus.qute;

/**
 * Maps keys to values in a similar way to {@link java.util.Map}. The difference is that it could be stateless, ie. the lookup
 * may be performed dynamically.
 * 
 * @see ValueResolvers#mapperResolver()
 */
public interface Mapper {

    Object get(String key);

}
