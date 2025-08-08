package io.quarkus.devui.runtime.mcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.devui.runtime.DevUIBuildTimeStaticService;
import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.runtime.mcp.model.resource.Content;
import io.quarkus.devui.runtime.mcp.model.resource.Resource;
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

    private final List<Resource> resources = new LinkedList<>();

    @PostConstruct
    public void init() {
        if (this.resources.isEmpty()) {
            addBuildTimeData();
            addRecordedData();
        }
    }

    @JsonRpcDescription("This list all resources available for MCP")
    public Map<String, List<Resource>> list() {
        return Map.of("resources", this.resources);
    }

    @JsonRpcDescription("This reads a certain resource given the uri as provided by resources/list")
    public Map<String, List<Content>> read(
            @JsonRpcDescription("The uri of the resources as defined in resources/list") String uri) {
        String subUri = uri.substring(URI_SCHEME.length());
        if (subUri.startsWith(SUB_SCHEME_BUILD_TIME)) {
            return readBuildTimeData(uri);
        } else if (subUri.startsWith(SUB_SCHEME_RECORDED)) {
            return readRecordedData(uri);
        } else {
            throw new UncheckedIOException(uri + " not found", new IOException());
        }
    }

    private Map<String, List<Content>> readBuildTimeData(String uri) {
        JsonMapper jsonMapper = jsonRpcRouter.getJsonMapper();

        String method = uri.substring((URI_SCHEME + SUB_SCHEME_BUILD_TIME).length());
        String[] split = method.split(UNDERSCORE);
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

    private void addBuildTimeData() {
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
                            if (descriptions.containsKey(methodName)) {
                                Resource resource = new Resource();
                                resource.uri = URI_SCHEME + SUB_SCHEME_BUILD_TIME + methodName;
                                resource.name = methodName;
                                resource.description = descriptions.get(methodName);
                                resource.mimeType = contentTypes.get(methodName);
                                this.resources.add(resource);
                            }
                        }
                    }
                } catch (IOException ex) {
                    // Ignore ?
                    ex.printStackTrace();
                }
            }
        }
    }

    private void addRecordedData() {
        Map<String, JsonRpcMethod> recordedMethodsMap = jsonRpcRouter.getRecordedMethodsMap();

        for (JsonRpcMethod recordedJsonRpcMethod : recordedMethodsMap.values()) {
            if (recordedJsonRpcMethod.getUsage().contains(Usage.DEV_MCP)) {
                Resource resource = new Resource();
                resource.uri = URI_SCHEME + SUB_SCHEME_RECORDED + recordedJsonRpcMethod.getMethodName();
                resource.name = recordedJsonRpcMethod.getMethodName();

                if (recordedJsonRpcMethod.getDescription() != null && !recordedJsonRpcMethod.getDescription().isBlank()) {
                    resource.description = recordedJsonRpcMethod.getDescription();
                }
                this.resources.add(resource);
            }
        }

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
