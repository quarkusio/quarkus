package io.quarkus.jfr.runtime.rest;

import io.quarkus.jfr.runtime.RequestId;

public abstract class AbstractHttpReactiveStartEvent<ID extends RequestId> extends AbstractHttpEvent<ID> {
}
