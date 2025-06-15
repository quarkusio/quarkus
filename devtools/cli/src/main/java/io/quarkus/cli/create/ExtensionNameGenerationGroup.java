package io.quarkus.cli.create;

import io.quarkus.devtools.commands.CreateProjectHelper;
import picocli.CommandLine;

public class ExtensionNameGenerationGroup {
    String packageName;

    @CommandLine.Option(paramLabel = "NAMESPACE-ID", names = { "-N",
            "--namespace-id" }, description = "A common prefix for all module artifactIds")
    String namespaceId;

    @CommandLine.Option(paramLabel = "EXTENSION-NAME", names = { "--extension-name" }, description = "Extension name")
    String extensionName;

    @CommandLine.Option(paramLabel = "EXTENSION-DESCRIPTION", names = {
            "--extension-description" }, description = "Extension description")
    String extensionDescription;

    @CommandLine.Option(paramLabel = "NAMESPACE-NAME", names = {
            "--namespace-name" }, description = "A common prefix for all module names")
    String namespaceName;

    @CommandLine.Option(paramLabel = "PACKAGE-NAME", names = {
            "--package-name" }, description = "Base package for generated classes")
    void setPackageName(String name) {
        this.packageName = CreateProjectHelper.checkPackageName(name);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public String extensionDescription() {
        return extensionDescription;
    }

    public String getNamespaceId() {
        return namespaceId;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    @Override
    public String toString() {
        return "ExtensionNameGenerationGroup [" + "extensionName=" + extensionName + ", extensionDescription="
                + extensionDescription + ", namespaceId=" + namespaceId + ", namespaceName=" + namespaceName + "]";
    }
}
