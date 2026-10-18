package io.quarkus.devtools.codestarts;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CodestartProjectDefinition {

    void generate(Path targetDirectory) throws IOException;

    List<Codestart> getCodestarts();

    CodestartProjectInput getProjectInput();

    Optional<Codestart> getCodestart(CodestartType type);

    Codestart getRequiredCodestart(CodestartType type);

    String getLanguageName();

    Map<String, Object> getSharedData();

    Map<String, Object> getDepsData();

    Map<String, Object> getCodestartProjectData();

    List<Codestart> getBaseCodestarts();

    List<Codestart> getExtraCodestarts();
}
