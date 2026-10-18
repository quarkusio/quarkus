package io.quarkus.websockets.next.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.websockets.next.deployment.CallbackArgument.ParameterContext;

final class CallbackArgumentsBuildItem extends SimpleBuildItem {

    final List<CallbackArgument> sortedArguments;

    CallbackArgumentsBuildItem(List<CallbackArgument> providers) {
        this.sortedArguments = providers;
    }

    /**
     *
     * @param context
     * @return all matching providers, never {@code null}
     */
    List<CallbackArgument> findMatching(ParameterContext context) {
        List<CallbackArgument> matching = new ArrayList<>();
        for (CallbackArgument argument : sortedArguments) {
            if (argument.matches(context)) {
                matching.add(argument);
            }
        }
        return matching;
    }

}
