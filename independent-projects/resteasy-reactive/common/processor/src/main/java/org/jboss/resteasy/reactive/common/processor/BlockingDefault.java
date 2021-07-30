package org.jboss.resteasy.reactive.common.processor;

public enum BlockingDefault {
    /**
     * The nature of the method is determined by the return type
     */
    AUTOMATIC,
    BLOCKING,
    NON_BLOCKING
}
