package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.console.QuarkusCommand;
import io.quarkus.devui.deployment.ide.IdeProcessor;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public class ResteasyReactiveDevModeProcessor {

    private static final Set<String> knownPaths = Collections.synchronizedSet(new HashSet<>());

    @BuildStep(onlyIf = IsDevelopment.class)
    ConsoleCommandBuildItem openCommand(HttpRootPathBuildItem rp, NonApplicationRootPathBuildItem np,
            SetupEndpointsResultBuildItem setupEndpointsResultBuildItem) {
        knownPaths.clear();
        for (ResourceClass clazz : setupEndpointsResultBuildItem.getResourceClasses()) {
            String cp = clazz.getPath();
            if (!cp.startsWith("/")) {
                cp = "/" + cp;
            }
            for (ResourceMethod method : clazz.getMethods()) {
                if (method.getHttpMethod() == null) {
                    continue;
                }
                if (!method.getHttpMethod().equals("GET")) {
                    continue;
                }
                if (method.getPath() == null || method.getPath().equals("") || method.getPath().equals("/")) {
                    knownPaths.add(cp);
                } else if (cp.endsWith("/")) {
                    if (method.getPath().startsWith("/")) {
                        knownPaths.add(cp + method.getPath().substring(1));
                    } else {
                        knownPaths.add(cp + method.getPath());
                    }
                } else {
                    if (method.getPath().startsWith("/")) {
                        knownPaths.add(cp + method.getPath());
                    } else {
                        knownPaths.add(cp + "/" + method.getPath());
                    }
                }
            }
        }
        var c = ConfigProvider.getConfig();
        String host = c.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        String port = c.getOptionalValue("quarkus.http.port", String.class).orElse("8080");
        return new ConsoleCommandBuildItem(new OpenCommand(rp, np, host, port));
    }

    @CommandDefinition(name = "open", description = "Opens a path in a web browser")
    public static class OpenCommand extends QuarkusCommand {

        @Argument(required = true, completer = PathCompleter.class)
        private String url;

        final HttpRootPathBuildItem rp;
        final NonApplicationRootPathBuildItem np;
        final String host;
        final String port;

        public OpenCommand(HttpRootPathBuildItem rp, NonApplicationRootPathBuildItem np, String host, String port) {
            this.rp = rp;
            this.np = np;
            this.host = host;
            this.port = port;
        }

        @Override
        public CommandResult doExecute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            IdeProcessor.openBrowser(rp, np, url.startsWith("/") ? url : "/" + url, host, port);
            return CommandResult.SUCCESS;
        }
    }

    public static class PathCompleter implements OptionCompleter {

        @Override
        public void complete(CompleterInvocation completerInvocation) {
            CompletionResult result = complete(knownPaths, completerInvocation.getGivenCompleteValue());
            completerInvocation.setAppendSpace(result.appendSpace);
            completerInvocation.setCompleterValues(result.results);
        }

        /**
         * performs tab completions for the known paths
         */
        public static CompletionResult complete(Set<String> knownPaths, String soFar) {
            boolean appendSpace = true;
            Set<String> getRoutes = new HashSet<>(knownPaths);
            String url = soFar;
            if (!url.startsWith("/")) {
                url = "/" + url;
            }
            Set<String> toAdd = new HashSet<>();
            for (String route : getRoutes) {
                //parse them into template components
                URITemplate template = new URITemplate(route, false);
                URITemplate.TemplateComponent[] components = template.components;
                int urlPos = 0;
                boolean done = false;
                for (int i = 0; i < components.length; i++) {
                    //iterate over the components and see if they can match the current URL
                    URITemplate.TemplateComponent component = components[i];
                    if (component.type == URITemplate.Type.LITERAL) {
                        int componentPos = 0;
                        while (componentPos < component.literalText.length() && urlPos < url.length()) {
                            //check for literal matches
                            if (url.charAt(urlPos++) != component.literalText.charAt(componentPos++)) {
                                done = true;
                                break;
                            }
                        }
                        if (urlPos == url.length() && componentPos != component.literalText.length()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(url);
                            sb.append(component.literalText.substring(componentPos));
                            if (i != components.length - 1) {
                                //if we are not the end add the next segment as well
                                sb.append(components[i + 1].stringRepresentation());
                                //don't append a space, we are not the end
                                appendSpace = false;
                            }

                            toAdd.add(sb.toString());
                            done = true;
                        }
                    } else {
                        //this is not 100% for CUSTOM_REGEX, as it may not terminate on /
                        //close enough for this though
                        boolean found = false;
                        int startingUrlPos = urlPos;
                        while (urlPos < url.length()) {
                            //loop till we find a /
                            if (url.charAt(urlPos) == '/') {
                                found = true;
                                break;
                            }
                            urlPos++;
                        }
                        if (!found) {
                            toAdd.add(url.substring(0, startingUrlPos) + "{" + component.name + "}");
                            appendSpace = false;
                            done = true;
                        }
                    }

                    if (done) {
                        break;
                    }
                }
            }
            return new CompletionResult(appendSpace, toAdd);
        }
    }

    public static class CompletionResult {
        final boolean appendSpace;
        final Set<String> results;

        public CompletionResult(boolean appendSpace, Set<String> results) {
            this.appendSpace = appendSpace;
            this.results = results;
        }

        public boolean isAppendSpace() {
            return appendSpace;
        }

        public Set<String> getResults() {
            return results;
        }
    }
}
