package org.jboss.resteasy.reactive.common.jaxrs;

/**
 * Determines how multiple values of the same {@code application/x-www-form-urlencoded} form parameter are encoded
 */
public enum MultiFormParamMode {
    /**
     * <code>foo=v1&amp;foo=v2&amp;foo=v3</code>
     */
    MULTI_PAIRS,
    /**
     * <code>foo=v1,v2,v3</code>
     */
    COMMA_SEPARATED,
    /**
     * <code>foo[]=v1&amp;foo[]=v2&amp;foo[]=v3</code>
     */
    ARRAY_PAIRS
}
