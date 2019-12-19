package io.quarkus.jberet.runtime;

import java.util.HashMap;
import java.util.Map;

import org.jberet.job.model.Job;

class JobDefinitionRepository {

    private Map<String, Job> jobDefinitions;

    JobDefinitionRepository() {
        jobDefinitions = new HashMap<>();
    }

    void addJobDefinition(String jobXmlName, Job definition) {
        jobDefinitions.put(jobXmlName, definition);
    }

    Job getJobDefinition(String jobXmlName) {
        return jobDefinitions.get(jobXmlName);
    }

}
