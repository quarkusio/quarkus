package io.quarkus.maven.components;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;

import io.quarkus.maven.QuarkusBootstrapProvider;

@Singleton
@Named("quarkus-bootstrap")
public class BootstrapSessionListener extends AbstractMavenLifecycleParticipant {

    private final QuarkusBootstrapProvider bootstrapProvider;

    private boolean enabled;

    @Inject
    public BootstrapSessionListener(QuarkusBootstrapProvider bootstrapProvider) {
        this.bootstrapProvider = bootstrapProvider;
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        try {
            bootstrapProvider.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterProjectsRead(MavenSession session)
            throws MavenExecutionException {
        // if this method is called then Maven plugin extensions are enabled
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
