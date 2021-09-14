package io.quarkus.devtools.codestarts.core.strategy;

import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.core.CodestartData;
import io.quarkus.devtools.codestarts.core.reader.TargetFile;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class SmartPackageFileStrategyHandler implements CodestartFileStrategyHandler {

    private static final String DEFAULT_PACKAGE_DIR = "org/acme";
    private static final String REPLACE_REGEX = "((import)|(package)) org\\.acme";

    @Override
    public String name() {
        return "smart-package";
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<TargetFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);

        final Optional<String> optPackageName = NestedMaps.getValue(data, "project.package-name");
        String modifiedRelativePath = relativePath;
        String content = codestartFiles.get(0).getContent();
        if (optPackageName.isPresent()) {
            final String packageName = optPackageName.get();
            final String packageNameAsDir = packageName.replace('.', '/');
            modifiedRelativePath = relativePath.replace(DEFAULT_PACKAGE_DIR, packageNameAsDir);
            content = refactorPackage(content, packageName);
        }

        // TODO this is a temporary workaround, we need change how this work to support having multiple processors for one file
        if (modifiedRelativePath.contains("/native-test/")) {
            final boolean isMaven = CodestartData.getBuildtool(data).filter(b -> Objects.equals(b, "maven")).isPresent();
            if (isMaven) {
                modifiedRelativePath = modifiedRelativePath.replace("/native-test/", "/test/");
            }
        }

        final Path targetPath = targetDirectory.resolve(modifiedRelativePath);

        if (Files.exists(targetPath)) {
            throw new CodestartStructureException(
                    "File already exists: " + targetPath.toString());
        }

        createDirectories(targetPath);
        writeFile(targetPath, content);
    }

    static String refactorPackage(String content, String packageName) {
        return content.replaceAll(REPLACE_REGEX, "$1 " + packageName);
    }
}
