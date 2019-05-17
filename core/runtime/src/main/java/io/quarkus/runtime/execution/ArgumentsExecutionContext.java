package io.quarkus.runtime.execution;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.wildfly.common.Assert;

/**
 * The execution context which holds the command line arguments that were passed in to the program.
 */
public final class ArgumentsExecutionContext extends ExecutionContext implements Iterable<String> {
    /**
     * A constant array of zero arguments.
     */
    public static final String[] NO_ARGUMENTS = new String[0];

    private final String[] args;
    private List<String> asList;

    ArgumentsExecutionContext(final ExecutionContext executionContext, final String[] args, final boolean clone) {
        super(executionContext);
        Assert.checkNotNullParam("args", args);
        this.args = args.length == 0 ? NO_ARGUMENTS : clone ? args.clone() : args;
    }

    /**
     * Construct a new instance.
     *
     * @param parent the parent context (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     */
    public ArgumentsExecutionContext(final ExecutionContext parent, final String[] args) {
        this(parent, args, true);
    }

    public int getArgumentCount() {
        return args.length;
    }

    public String getArgument(int index) {
        return args[index];
    }

    public String[] getArgumentsAsArray() {
        return args.length == 0 ? NO_ARGUMENTS : args.clone();
    }

    public List<String> getArgumentsAsList() {
        List<String> asList = this.asList;
        if (asList == null) {
            this.asList = asList = Collections.unmodifiableList(Arrays.asList(getArgumentsAsArray()));
        }
        return asList;
    }

    public Iterator<String> iterator() {
        return getArgumentsAsList().iterator();
    }
}
