package io.quarkus.devtools.codestarts.core.strategy;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.model.Model;

import io.fabric8.maven.Maven;
import io.fabric8.maven.XMLFormat;
import io.fabric8.maven.merge.SmartModelMerger;
import io.quarkus.devtools.codestarts.CodestartException;
import io.quarkus.devtools.codestarts.core.CodestartData;
import io.quarkus.devtools.codestarts.core.reader.TargetFile;

final class SmartPomMergeCodestartFileStrategyHandler implements CodestartFileStrategyHandler {

    @Override
    public String name() {
        return "smart-pom-merge";
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<TargetFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        final Path targetPath = targetDirectory.resolve(relativePath);
        checkTargetDoesNotExist(targetPath);
        createDirectories(targetPath);
        CodestartData.getBuildtool(data)
                .filter(b -> Objects.equals(b, "maven"))
                .orElseThrow(() -> new CodestartException(
                        "something is wrong, smart-pom-merge file strategy must only be used on maven projects"));

        final SmartModelMerger merger = new SmartModelMerger();
        final Model targetModel = Maven.readModel(new StringReader(codestartFiles.get(0).getContent()));
        final ListIterator<TargetFile> iterator = codestartFiles.listIterator(1);
        while (iterator.hasNext()) {
            merger.merge(targetModel, Maven.readModel(new StringReader(iterator.next().getContent())), true, null);
        }
        Maven.writeModel(targetModel, targetPath,
                XMLFormat.builder().indent("    ").insertLineBreakBetweenMajorSections().build());
    }
}
