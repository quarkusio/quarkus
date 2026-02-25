package io.quarkus.devui.runtime.mcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.devui.runtime.DevUIBuildTimeStaticService;
import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.runtime.mcp.model.resource.Content;
import io.quarkus.devui.runtime.mcp.model.resource.Resource;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.Usage;

/**
 * This expose all Dev UI BuildTimeData and Recorded values as MCP Resources
 *
 * @see https://modelcontextprotocol.io/specification/2024-11-05/server/resources#listing-resources
 * @see https://modelcontextprotocol.io/specification/2024-11-05/server/resources#reading-resources
 *
 *      TODO: We can also expose the WorkItems here using resourceTemplates. See
 *      https://modelcontextprotocol.io/specification/2024-11-05/server/resources#resource-templates
 *      TODO: We can also send a changed notification on hot reload. See
 *      https://modelcontextprotocol.io/specification/2024-11-05/server/resources#list-changed-notification
 */
@ApplicationScoped
public class McpResourcesService {

    @Inject
    JsonRpcRouter jsonRpcRouter;

    @Inject
    DevUIBuildTimeStaticService buildTimeStaticService;

    @Inject
    McpDevUIJsonRpcService mcpDevUIJsonRpcService;

    @DevMCPEnableByDefault
    @JsonRpcDescription(value = "This list all resources available for MCP")
    public Map<String, List<Resource>> list() {
        return listAny(Filter.enabled);
    }

    public Map<String, List<Resource>> listDisabled() {
        return listAny(Filter.disabled);
    }

    private Map<String, List<Resource>> listAny(Filter filter) {
        if (mcpDevUIJsonRpcService.getMcpServerConfiguration().isEnabled()) {

            List<Resource> resources = new ArrayList<>();
            resources.addAll(getBuildTimeData(filter));
            resources.addAll(getRecordedData(filter));

            return Map.of("resources", resources);
        }
        return null;
    }

    public boolean disableResource(String name) {
        mcpDevUIJsonRpcService.disableMethod(name);
        return true;
    }

    public boolean enableResource(String name) {
        mcpDevUIJsonRpcService.enableMethod(name);
        return true;
    }

    @DevMCPEnableByDefault
    @JsonRpcDescription("This reads a certain resource given the uri as provided by resources/list")
    public Map<String, List<Content>> read(
            @JsonRpcDescription("The uri of the resources as defined in resources/list") String uri) {
        if (mcpDevUIJsonRpcService.getMcpServerConfiguration().isEnabled()) {
            if (uri == null || !uri.startsWith(URI_SCHEME)) {
                throw new IllegalArgumentException(
                        "Invalid resource URI: " + uri + ". Expected URI starting with: " + URI_SCHEME);
            }
            String subUri = uri.substring(URI_SCHEME.length());
            if (subUri.startsWith(SUB_SCHEME_BUILD_TIME)) {
                return readBuildTimeData(uri);
            } else if (subUri.startsWith(SUB_SCHEME_RECORDED)) {
                return readRecordedData(uri);
            } else {
                throw new UncheckedIOException(uri + " not found", new IOException());
            }
        }
        return null;
    }

    private Map<String, List<Content>> readBuildTimeData(String uri) {
        JsonMapper jsonMapper = jsonRpcRouter.getJsonMapper();

        String method = uri.substring((URI_SCHEME + SUB_SCHEME_BUILD_TIME).length());
        String[] split = method.split(UNDERSCORE);
        if (split.length < 2) {
            throw new IllegalArgumentException(
                    "Invalid build-time resource URI: " + uri + ". Expected format: " + URI_SCHEME + SUB_SCHEME_BUILD_TIME
                            + "<namespace>_<name>");
        }
        String ns = split[0];
        String constt = split[1];
        String filename = ns + DASH_DATA_DOT_JS;
        String path = buildTimeStaticService.getUrlAndPath().get(filename);
        try {
            String jsContent = Files.readString(Paths.get(path));
            Content content = new Content();
            content.uri = uri;
            content.text = getBuildTimeDataConstValue(jsContent, constt);
            return Map.of("contents", List.of(content));
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read " + path, ex);
        }

    }

    private Map<String, List<Content>> readRecordedData(String uri) {
        Map<String, JsonRpcMethod> recordedMethodsMap = jsonRpcRouter.getRecordedMethodsMap();
        JsonMapper jsonMapper = jsonRpcRouter.getJsonMapper();

        String method = uri.substring((URI_SCHEME + SUB_SCHEME_RECORDED).length());
        Content content = new Content();
        content.uri = uri;
        content.text = jsonMapper.toString(recordedMethodsMap.get(method).getRuntimeValue().getValue(), true);
        // TODO: Handle Futures Unis and Multies

        return Map.of("contents", List.of(content));
    }

    private List<Resource> getBuildTimeData(Filter filter) {
        List<Resource> r = new ArrayList<>();
        Map<String, String> descriptions = buildTimeStaticService.getDescriptions();
        Map<String, String> contentTypes = buildTimeStaticService.getContentTypes();

        Map<String, String> urlAndPath = buildTimeStaticService.getUrlAndPath();
        for (Map.Entry<String, String> kv : urlAndPath.entrySet()) {
            if (kv.getKey().endsWith(DASH_DATA_DOT_JS)) {
                try {
                    String key = kv.getKey().substring(0, kv.getKey().length() - DASH_DATA_DOT_JS.length());

                    if (!key.equalsIgnoreCase("devui-jsonrpc")) { // We ignore this namespace, as this is the same as tools/list
                        String content = Files.readString(Paths.get(kv.getValue()));
                        Set<String> methodNames = extractBuildTimeDataMethods(key, content);
                        for (String methodName : methodNames) {
                            if (descriptions.containsKey(methodName) && isEnabled(methodName, filter)) {
                                Resource resource = new Resource();
                                resource.uri = URI_SCHEME + SUB_SCHEME_BUILD_TIME + methodName;
                                resource.name = methodName;
                                resource.description = descriptions.get(methodName);
                                resource.mimeType = contentTypes.get(methodName);
                                r.add(resource);
                            }
                        }
                    }
                } catch (IOException ex) {
                    // Ignore ?
                    ex.printStackTrace();
                }
            }
        }
        return r;
    }

    private List<Resource> getRecordedData(Filter filter) {
        List<Resource> r = new ArrayList<>();
        Map<String, JsonRpcMethod> recordedMethodsMap = jsonRpcRouter.getRecordedMethodsMap();

        for (JsonRpcMethod recordedJsonRpcMethod : recordedMethodsMap.values()) {
            if (isEnabled(recordedJsonRpcMethod, filter)) {
                Resource resource = new Resource();
                resource.uri = URI_SCHEME + SUB_SCHEME_RECORDED + recordedJsonRpcMethod.getMethodName();
                resource.name = recordedJsonRpcMethod.getMethodName();

                if (recordedJsonRpcMethod.getDescription() != null && !recordedJsonRpcMethod.getDescription().isBlank()) {
                    resource.description = recordedJsonRpcMethod.getDescription();
                }
                r.add(resource);
            }
        }
        return r;
    }

    private boolean isEnabled(JsonRpcMethod method, Filter filter) {

        if (method.getUsage().contains(Usage.DEV_MCP)) {
            if (mcpDevUIJsonRpcService.isExplicitlyEnabled(method.getMethodName())) {
                return filter.equals(Filter.enabled);
            } else if (mcpDevUIJsonRpcService.isExplicitlyDisabled(method.getMethodName())) {
                return filter.equals(Filter.disabled);
            } else if (filter.equals(Filter.enabled)) {
                return method.isMcpEnabledByDefault();
            } else if (filter.equals(Filter.disabled)) {
                return !method.isMcpEnabledByDefault();
            }
        }
        return false;
    }

    private boolean isEnabled(String method, Filter filter) {

        Map<String, String> mcpDefaultEnabled = buildTimeStaticService.getMcpDefaultEnabled();

        if (mcpDevUIJsonRpcService.isExplicitlyEnabled(method)) {
            return filter.equals(Filter.enabled);
        } else if (mcpDevUIJsonRpcService.isExplicitlyDisabled(method)) {
            return filter.equals(Filter.disabled);
        } else if (filter.equals(Filter.enabled)) {
            return (!mcpDefaultEnabled.containsKey(method) || mcpDefaultEnabled.get(method).equals("true"));
        } else if (filter.equals(Filter.disabled)) {
            return (mcpDefaultEnabled.containsKey(method) && mcpDefaultEnabled.get(method).equals("false"));
        }
        return false;
    }

    private Set<String> extractBuildTimeDataMethods(String ns, String jsContent) {
        Set<String> result = new LinkedHashSet<>();

        Matcher matcher = BTD_PATTERN.matcher(jsContent);

        while (matcher.find()) {
            String name = matcher.group(1);
            String jsonValue = matcher.group(2).trim();
            if (!isEmptyValue(jsonValue)) { // No worth in adding empty resources
                result.add(ns + UNDERSCORE + name);
            }
        }

        return result;
    }

    private boolean isEmptyValue(String value) {
        return value == null ||
                value.trim().isEmpty() ||
                value.trim().matches("^\\[\\s*]$");
    }

    private String getBuildTimeDataConstValue(String jsContent, String constName) {
        String patternString = "export const " + Pattern.quote(constName) + "\\s*=\\s*([^;]+);";
        Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsContent);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "Error: Data not found for " + constName;
    }

    private static final Pattern BTD_PATTERN = Pattern.compile("export const (\\w+)\\s*=\\s*([^;]+);", Pattern.DOTALL);

    private static final String URI_SCHEME = "quarkus://resource/";
    private static final String SUB_SCHEME_BUILD_TIME = "build-time/";
    private static final String SUB_SCHEME_RECORDED = "recorded/";
    private static final String UNDERSCORE = "_";
    private static final String DASH_DATA_DOT_JS = "-data.js";
}
