package io.quarkus.rest.client.reactive.redirect;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

@RegisterProvider(EnablePostRedirectHandler.class)
public interface RedirectingResourceWithCustomRedirectHandlerClient extends RedirectingResourceClient {
}
