package io.quarkus.gradle.tasks;

import io.quarkus.cli.commands.QuarkusCommandInvocation;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.MessageWriter;

public abstract class QuarkusPlatformTask extends QuarkusTask {

    QuarkusPlatformTask(String description) {
        super(description);
    }

    protected void execute() {
        final MessageWriter msgWriter = new GradleMessageWriter(getProject().getLogger());
        final QuarkusPlatformDescriptor platformDescr = getPlatformDescriptor(msgWriter);
        doExecute(new QuarkusCommandInvocation(platformDescr, msgWriter));
    }

    protected abstract void doExecute(QuarkusCommandInvocation invocation);

    private QuarkusPlatformDescriptor getPlatformDescriptor(MessageWriter msgWriter) {
        return QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setArtifactResolver(extension().resolveAppModel())
                .setMessageWriter(msgWriter)
                .resolveFromBom(
                        getRequiredProperty("quarkusPlatformGroupId"),
                        getRequiredProperty("quarkusPlatformArtifactId"),
                        getRequiredProperty("quarkusPlatformVersion"));
    }

    private String getRequiredProperty(String name) {
        final String value = (String) getProject().findProperty(name);
        if (value == null) {
            throw new IllegalStateException("Required property " + name + " is missing from gradle.properties");
        }
        return value;
    }
}
