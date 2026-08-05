package io.quarkus.tck.rest;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class QuarkusRestTckArchiveProcessor {

    @Inject
    private Instance<ProtocolMetaData> protocolMetaData;

    public void process(@Observes BeforeDeploy event) {
        Archive<?> archive = event.getDeployment().getArchive();
        if (!(archive instanceof WebArchive)) {
            return;
        }
        WebArchive war = (WebArchive) archive;
        String warName = archive.getName().replace(".war", "");

        StringBuilder props = new StringBuilder();

        props.append("quarkus.http.root-path=/").append(warName).append("\n");
        props.append("quarkus.http.test-port=0\n");
        props.append("quarkus.rest.single-default-produces=false\n");
        props.append("quarkus.rest.default-produces=false\n");
        props.append("quarkus.rest.fail-on-duplicate=false\n");

        if (warName.contains("securitycontext") || warName.contains("requestcontext_security")) {
            props.append("quarkus.http.auth.basic=true\n");
            props.append("quarkus.security.users.embedded.enabled=true\n");
            props.append("quarkus.security.users.embedded.plain-text=true\n");
            props.append("quarkus.security.users.embedded.users.tck-staff=tck-staff-password\n");
            props.append("quarkus.security.users.embedded.roles.tck-staff=staff,mgr,DIRECTOR\n");
            props.append("quarkus.security.users.embedded.users.tck-user=tck-user-password\n");
            props.append("quarkus.security.users.embedded.roles.tck-user=guest,OTHERROLE\n");
            props.append("quarkus.http.auth.policy.tck-role-policy.roles-allowed=DIRECTOR,OTHERROLE\n");
        }

        if (warName.contains("securitycontext_basic")) {
            props.append("quarkus.http.auth.permission.secured.paths=Servlet/*\n");
            props.append("quarkus.http.auth.permission.secured.methods=GET,POST,HEAD,PUT\n");
            props.append("quarkus.http.auth.permission.secured.policy=tck-role-policy\n");
        } else if (warName.contains("requestcontext_security")) {
            props.append("quarkus.http.auth.permission.secured.paths=resource/*\n");
            props.append("quarkus.http.auth.permission.secured.methods=GET,POST\n");
            props.append("quarkus.http.auth.permission.secured.policy=tck-role-policy\n");
        }

        war.addAsResource(new StringAsset(props.toString()), "application.properties");
    }

    public void updatePort(@Observes AfterDeploy event) {
        ProtocolMetaData metaData = protocolMetaData.get();
        if (metaData == null || !metaData.hasContext(HTTPContext.class)) {
            return;
        }
        HTTPContext context = metaData.getContexts(HTTPContext.class).iterator().next();
        System.setProperty("webServerPort", String.valueOf(context.getPort()));
    }
}
