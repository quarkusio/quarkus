package org.jboss.resteasy.reactive.common.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple parser intended to parse sequences of name/value pairs but keeping the order in which the parameters are
 * declared.
 */

public class OrderedParameterParser extends ParameterParser {

    @Override
    protected <K, V> Map<K, V> newMap() {
        return new LinkedHashMap<>();
    }
}
