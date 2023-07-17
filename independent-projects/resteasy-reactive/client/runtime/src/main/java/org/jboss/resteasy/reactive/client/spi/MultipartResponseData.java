package org.jboss.resteasy.reactive.client.spi;

import java.util.List;

public interface MultipartResponseData {
    <T> T newInstance();

    List<FieldFiller> getFieldFillers();
}
