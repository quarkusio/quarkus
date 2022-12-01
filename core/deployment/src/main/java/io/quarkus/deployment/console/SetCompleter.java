package io.quarkus.deployment.console;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;

/**
 * option completer that takes a simple set of possible values
 */
public abstract class SetCompleter implements OptionCompleter<CompleterInvocation> {

    protected abstract Set<String> allOptions(String soFar);

    @Override
    public final void complete(CompleterInvocation invocation) {
        Collection<String> all = allOptions(invocation.getGivenCompleteValue());
        completeFromSet(invocation, new TreeSet<>(all));
    }

    public void completeFromSet(CompleterInvocation invocation, Set<String> all) {
        String v = invocation.getGivenCompleteValue();
        if (v == null || v.length() == 0) {
            invocation.addAllCompleterValues(all);
        } else {
            for (String item : all) {
                if (item.startsWith(v)) {
                    invocation.addCompleterValue(item);
                }
            }
        }
    }
}
