package io.quarkus.deployment.recording;

import java.util.Objects;

public class JobParameter {
    public static final JobParameter JobContext = new JobParameter(JobParameter.class);

    private String className;
    private String actualClassName;
    private Object object;

    private JobParameter() {
        // used for deserialization
    }

    private JobParameter(Class<?> clazz) {
        this(clazz.getName(), null);
    }

    public JobParameter(Class<?> clazz, Object object) {
        this(clazz.getName(), object);
    }

    public JobParameter(Object object) {
        this(object.getClass().getName(), object);
    }

    public JobParameter(String className, Object object) {
        this(className, isNotNullNorAnEnum(object) ? object.getClass().getName() : className, object);
    }

    public JobParameter(String className, String actualClassName, Object object) {
        this.className = className;
        this.actualClassName = actualClassName;
        this.object = object;
    }

    /**
     * Represents the class name expected by the job method (e.g. an object or an interface)
     *
     * @return the class name expected by the job method (e.g. an object or an interface)
     */
    public String getClassName() {
        return className;
    }

    /**
     * Represents the actual class name of the job parameter (e.g. an object), this will never be an interface
     *
     * @return the actual class name of the job parameter (e.g. an object), this will never be an interface
     */
    public String getActualClassName() {
        return actualClassName;
    }

    /**
     * The actual job parameter
     *
     * @return the actual job parameter
     */
    public Object getObject() {
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JobParameter)) {
            return false;
        }
        JobParameter that = (JobParameter) o;
        return Objects.equals(className, that.className)
                && Objects.equals(actualClassName, that.actualClassName)
                && Objects.equals(object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, actualClassName, object);
    }

    protected static boolean isNotNullNorAnEnum(Object object) {
        return object != null && !(object instanceof Enum);
    }
}
