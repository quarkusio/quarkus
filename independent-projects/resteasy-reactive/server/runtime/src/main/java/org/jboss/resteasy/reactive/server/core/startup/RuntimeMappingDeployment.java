package org.jboss.resteasy.reactive.server.core.startup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.server.handlers.MediaTypeMapper;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

class RuntimeMappingDeployment {

    private final Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> classTemplates;

    private final SortedMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> nullMethod;

    private String currentHttpMethod;
    private List<RequestMapper.RequestPath<RuntimeResource>> currentMapperPerMethodTemplates;

    private Map<String, RequestMapper<RuntimeResource>> classMapper;
    private int maxMethodTemplateNameCount = -1;

    RuntimeMappingDeployment(
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> classTemplates) {
        this.classTemplates = classTemplates;
        this.nullMethod = classTemplates.get(null);
    }

    int getMaxMethodTemplateNameCount() {
        if (maxMethodTemplateNameCount == -1) {
            throw new IllegalStateException("Method can only be called after 'buildClassMapper'");
        }
        return maxMethodTemplateNameCount;
    }

    Map<String, RequestMapper<RuntimeResource>> buildClassMapper() {
        classMapper = new HashMap<>();
        maxMethodTemplateNameCount = 0;
        classTemplates.forEach(this::forEachClassTemplate);
        return classMapper;
    }

    private void forEachClassTemplate(String httpMethod,
            TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> perMethodTemplateMap) {
        currentHttpMethod = httpMethod;

        if (nullMethod != null) {
            for (var nm : nullMethod.entrySet()) {
                if (!perMethodTemplateMap.containsKey(nm.getKey())) {
                    //resource methods take precedence
                    //just skip sub resource locators for now
                    //may need to be revisited if we want to pass the TCK 100%
                    perMethodTemplateMap.put(nm.getKey(), nm.getValue());
                }
            }
        }

        //now we have all our possible resources
        currentMapperPerMethodTemplates = new ArrayList<>();
        perMethodTemplateMap.forEach(this::forEachMethodTemplateMap);

        classMapper.put(httpMethod, new RequestMapper<>(currentMapperPerMethodTemplates));
    }

    private void forEachMethodTemplateMap(URITemplate path, List<RequestMapper.RequestPath<RuntimeResource>> requestPaths) {
        int methodTemplateNameCount = path.countPathParamNames();
        if (methodTemplateNameCount > maxMethodTemplateNameCount) {
            maxMethodTemplateNameCount = methodTemplateNameCount;
        }
        if (requestPaths.size() == 1) {
            //simple case, only one match
            currentMapperPerMethodTemplates.addAll(requestPaths);
        } else {
            List<RuntimeResource> resources = new ArrayList<>(requestPaths.size());
            for (int j = 0; j < requestPaths.size(); j++) {
                resources.add(requestPaths.get(j).value);
            }
            MediaTypeMapper mapper = new MediaTypeMapper(resources);
            //now we just create a fake RuntimeResource
            //we could add another layer of indirection, however this is not a common case
            //so we don't want to add any extra latency into the common case
            RuntimeResource fake = new RuntimeResource(currentHttpMethod, path, null, null, Collections.emptyList(),
                    null, null,
                    new ServerRestHandler[] { mapper }, null, new Class[0], null, false, null, null, null, null, null,
                    Collections.emptyMap());
            currentMapperPerMethodTemplates.add(new RequestMapper.RequestPath<>(false, fake.getPath(), fake));
        }
    }

    static void buildMethodMapper(
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers,
            ResourceMethod method, RuntimeResource runtimeResource) {
        TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> templateMap = perClassMappers
                .get(method.getHttpMethod());
        if (templateMap == null) {
            perClassMappers.put(method.getHttpMethod(), templateMap = new TreeMap<>());
        }
        List<RequestMapper.RequestPath<RuntimeResource>> list = templateMap.get(runtimeResource.getPath());
        if (list == null) {
            templateMap.put(runtimeResource.getPath(), list = new ArrayList<>());
        }
        list.add(new RequestMapper.RequestPath<>(method.getHttpMethod() == null, runtimeResource.getPath(),
                runtimeResource));
    }
}
