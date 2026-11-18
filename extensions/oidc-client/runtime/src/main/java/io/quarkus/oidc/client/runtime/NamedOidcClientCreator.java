package io.quarkus.oidc.client.runtime;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;

/**
 * Creates the {@link OidcClient} beans with the {@link NamedOidcClient} qualifier.
 */
public final class NamedOidcClientCreator implements BeanCreator<OidcClient> {

    public static final String CLIENT_NAME_PARAM = "clientName";

    @Override
    public OidcClient create(SyntheticCreationalContext<OidcClient> context) {
        String clientName = (String) context.getParams().get(CLIENT_NAME_PARAM);
        return context.getInjectedReference(OidcClients.class).getClient(clientName);
    }
}
