package org.jboss.resteasy.reactive.server.providers.serialisers.json;

import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;

public abstract class AbstractServerJsonMessageBodyReader extends AbstractJsonMessageBodyReader
        implements ServerMessageBodyReader<Object> {

}
