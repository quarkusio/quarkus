package io.quarkus.resteasy.reactive.links.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LinksProviderRecorder {

    public void setLinksContainer(LinksContainer linksContainer) {
        RestLinksProviderImpl.setLinksContainer(linksContainer);
    }

    public void setGetterAccessorsContainer(RuntimeValue<GetterAccessorsContainer> getterAccessorsContainer) {
        RestLinksProviderImpl.setGetterAccessorsContainer(getterAccessorsContainer.getValue());
    }
}
