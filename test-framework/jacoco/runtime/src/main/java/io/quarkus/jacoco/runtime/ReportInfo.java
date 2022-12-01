package io.quarkus.jacoco.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ReportInfo {

    public String reportDir;
    public String dataFile;
    public final List<String> savedData = new ArrayList<>();
    public Set<String> sourceDirectories;
    public Set<String> classFiles;
    public String artifactId;

}
