package io.quarkus.tck.rest;

import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class QuarkusRestTckArchiveProcessor {

    public void process(@Observes BeforeDeploy event) {
        Archive<?> archive = event.getDeployment().getArchive();
        if (!(archive instanceof WebArchive)) {
            return;
        }
        WebArchive war = (WebArchive) archive;
        String warName = archive.getName().replace(".war", "");

        StringBuilder props = new StringBuilder();

        props.append("quarkus.http.root-path=/").append(warName).append("\n");
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
        }

        war.addAsResource(new StringAsset(props.toString()), "application.properties");
    }
}
