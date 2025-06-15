package io.quarkus.deployment.recording;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JobDetails {

    private final String className;
    private final String staticFieldName;
    private final String methodName;
    private final ArrayList<JobParameter> jobParameters;
    private Boolean cacheable;

    private JobDetails() {
        this(null, null, null, null);
        // used for deserialization
    }

    public JobDetails(String className, String staticFieldName, String methodName, List<JobParameter> jobParameters) {
        this.className = className;
        this.staticFieldName = staticFieldName;
        this.methodName = methodName;
        this.jobParameters = new ArrayList<>(jobParameters);
        this.cacheable = false;
    }

    public String getClassName() {
        return className;
    }

    public String getStaticFieldName() {
        return staticFieldName;
    }

    public boolean hasStaticFieldName() {
        return staticFieldName != null;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<JobParameter> getJobParameters() {
        return unmodifiableList(jobParameters);
    }

    public Class[] getJobParameterTypes() {
        return jobParameters.stream().map(JobParameter::getClassName).toArray(Class[]::new);
    }

    public Object[] getJobParameterValues() {
        return jobParameters.stream().map(JobParameter::getObject).toArray();
    }

    public Boolean getCacheable() {
        return cacheable;
    }

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JobDetails))
            return false;
        JobDetails that = (JobDetails) o;
        return Objects.equals(className, that.className) && Objects.equals(staticFieldName, that.staticFieldName)
                && Objects.equals(methodName, that.methodName) && Objects.equals(jobParameters, that.jobParameters)
                && Objects.equals(cacheable, that.cacheable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, staticFieldName, methodName, jobParameters, cacheable);
    }
}
