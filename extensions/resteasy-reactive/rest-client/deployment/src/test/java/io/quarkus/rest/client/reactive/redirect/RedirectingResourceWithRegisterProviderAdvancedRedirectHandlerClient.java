package io.quarkus.rest.client.reactive.redirect;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

@RegisterProvider(EnablePostAdvancedRedirectHandler.class)
public interface RedirectingResourceWithRegisterProviderAdvancedRedirectHandlerClient extends RedirectingResourceClient302 {
}
