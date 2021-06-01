package io.quarkus.cli.create;

import picocli.CommandLine;

public class ExtensionNameGenerationGroup {
    String extensionName;
    String namespaceId;
    String namespaceName;

    @CommandLine.Option(paramLabel = "EXTENSION-NAME", names = { "--extension-name" }, description = "Extension name")
    void setExtensionName(String name) {
        this.extensionName = name;
    }

    @CommandLine.Option(paramLabel = "NAMESPACE-ID", names = { "-N",
            "--namespace-id" }, description = "A common prefix for all module artifactIds")
    void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    @CommandLine.Option(paramLabel = "NAMESPACE-NAME", names = {
            "--namespace-name" }, description = "A common prefix for all module names")
    void setNamespaceName(String name) {
        this.namespaceName = name;
    }

    @Override
    public String toString() {
        return "ExtensionNameGenerationGroup ["
                + "extensionName=" + extensionName
                + ", namespaceId=" + namespaceId
                + ", namespaceName=" + namespaceName + "]";
    }
}
