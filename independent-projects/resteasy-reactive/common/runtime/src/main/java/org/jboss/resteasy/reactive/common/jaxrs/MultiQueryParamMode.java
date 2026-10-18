package org.jboss.resteasy.reactive.common.jaxrs;

public enum MultiQueryParamMode {
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
