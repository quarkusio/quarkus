package org.jboss.resteasy.reactive.common.types;

/**
 * A marker interface that will be used by RESTEasy Reactive to determine if a list of MessageBodyWriter objects can be
 * effectively trimmed to the first item. This is not meant to be used by application code - this interface is added
 * automatically added by Quarkus
 */
public interface AllWriteableMarker {

}
