package io.quarkus.cli.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;

import io.quarkus.dependencies.Extension;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

public class ExtensionCompleter implements OptionCompleter<CompleterInvocation> {

    @Override
    public void complete(CompleterInvocation invocation) {
        if (invocation.getGivenCompleteValue().length() == 0) {
            invocation.addAllCompleterValues(
                    getAllExtensions().stream().map(Extension::getSimplifiedArtifactId).collect(Collectors.toList()));
        } else {
            for (Extension loadExtension : getAllExtensions()) {
                if (loadExtension.getSimplifiedArtifactId().startsWith(invocation.getGivenCompleteValue()))
                    invocation.addCompleterValue(loadExtension.getSimplifiedArtifactId());
            }
        }
    }

    private List<Extension> getAllExtensions() {
        return QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor().getExtensions();
    }
}
