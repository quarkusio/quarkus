package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.init.InitializeClassPreInitRunnable;

/**
 * A build item that can be used to register pre-init tasks that will be executed early in GeneratedMain.
 * <p>
 * For now, these tasks are only executed when using the AOT runner.
 * <p>
 * Pre-init is run at the very beginning of the GeneratedMain, and all tasks are submitted to a thread pool
 * that will execute the tasks in parallel so don't expect any execution ordering.
 * <p>
 * The tasks are submitted to the thread pool in the priority order: a lower value for priority will be submitted first.
 */
public final class PreInitRunnableBuildItem extends MultiBuildItem implements Comparable<PreInitRunnableBuildItem> {

    public static final int DEFAULT_PRIORITY = 100;

    // the model here is extremely naive and, if we need more parameters, we will need to make it more clever
    // let's keep it simple for now
    private final String runnableClassName;
    private final String parameter;
    private final int priority;

    private PreInitRunnableBuildItem(String runnableClassName, String parameter, int priority) {
        this.runnableClassName = runnableClassName;
        this.parameter = parameter;
        this.priority = priority;
    }

    public static PreInitRunnableBuildItem runnable(String runnableClassName) {
        return new PreInitRunnableBuildItem(runnableClassName, null, DEFAULT_PRIORITY);
    }

    public static PreInitRunnableBuildItem runnable(String runnableClassName, int priority) {
        return new PreInitRunnableBuildItem(runnableClassName, null, priority);
    }

    public static PreInitRunnableBuildItem initializeClass(String className) {
        return new PreInitRunnableBuildItem(InitializeClassPreInitRunnable.class.getName(), className, DEFAULT_PRIORITY);
    }

    public static PreInitRunnableBuildItem initializeClass(String className, int priority) {
        return new PreInitRunnableBuildItem(InitializeClassPreInitRunnable.class.getName(), className, priority);
    }

    public String getRunnableClassName() {
        return runnableClassName;
    }

    public String getParameter() {
        return parameter;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(PreInitRunnableBuildItem o) {
        int compare = Integer.compare(priority, o.priority);

        if (compare != 0) {
            return compare;
        }

        compare = runnableClassName.compareTo(o.runnableClassName);

        if (compare != 0) {
            return compare;
        }

        if (parameter == null && o.parameter == null) {
            return 0;
        }
        if (parameter == null) {
            return 1; // nulls last
        }
        if (o.parameter == null) {
            return -1;
        }
        return parameter.compareTo(o.parameter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PreInitRunnableBuildItem other = (PreInitRunnableBuildItem) o;
        return runnableClassName.equals(other.runnableClassName)
                && Objects.equals(parameter, other.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runnableClassName, parameter);
    }
}
