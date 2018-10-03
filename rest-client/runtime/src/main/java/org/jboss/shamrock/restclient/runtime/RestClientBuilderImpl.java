/**
 * Copyright 2015-2017 Red Hat, Inc, and individual contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.shamrock.restclient.runtime;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;


/**
 * Created by hbraun on 15.01.18.
 */
class RestClientBuilderImpl implements RestClientBuilder {

    private static final String RESTEASY_PROPERTY_PREFIX = "resteasy.";

    private static final String DEFAULT_MAPPER_PROP = "microprofile.rest.client.disable.default.mapper";
    private static final Logger log = Logger.getLogger("org.jboss.shamrock.restclient");

    RestClientBuilderImpl() {
        ClientBuilder availableBuilder = ClientBuilder.newBuilder();

        if (availableBuilder instanceof ResteasyClientBuilder) {
            this.builderDelegate = (ResteasyClientBuilder) availableBuilder;
            this.configurationWrapper = new ConfigurationWrapper(this.builderDelegate.getConfiguration());
            this.config = ConfigProvider.getConfig();
        } else {
            throw new IllegalStateException("Incompatible client builder found " + availableBuilder.getClass());
        }
    }

    public Configuration getConfigurationWrapper() {
        return this.configurationWrapper;
    }

    @Override
    public RestClientBuilder baseUrl(URL url) {
        try {
            this.baseURI = url.toURI();
            return this;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T build(Class<T> aClass) throws IllegalStateException, RestClientDefinitionException {

        // Interface validity
        verifyInterface(aClass);

        // Provider annotations
        Annotation[] providers = aClass.getAnnotations();

        for (Annotation provider : providers) {
            if(provider instanceof  RegisterProvider) {
                RegisterProvider p = (RegisterProvider) provider;
                register(p.value(), p.priority());
            }
        }

        // Default exception mapper
        if (!isMapperDisabled()) {
            register(DefaultResponseExceptionMapper.class);
        }

        this.builderDelegate.register(new ExceptionMapping(localProviderInstances), 1);

        ClassLoader classLoader = aClass.getClassLoader();

        List<String> noProxyHosts = Arrays.asList(
                System.getProperty("http.nonProxyHosts", "localhost|127.*|[::1]").split("|"));
        String proxyHost = System.getProperty("http.proxyHost");

        T actualClient;
        ResteasyClient client;
        //TODO: Substrate does not support SSL yet
        this.builderDelegate.sslContext(new SSLContext(new SSLContextSpi() {
            @Override
            protected void engineInit(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom secureRandom) throws KeyManagementException {

            }

            @Override
            protected SSLSocketFactory engineGetSocketFactory() {
                return new SSLSocketFactory() {
                    @Override
                    public String[] getDefaultCipherSuites() {
                        return new String[0];
                    }

                    @Override
                    public String[] getSupportedCipherSuites() {
                        return new String[0];
                    }

                    @Override
                    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
                        return null;
                    }

                    @Override
                    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
                        return null;
                    }

                    @Override
                    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
                        return null;
                    }

                    @Override
                    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
                        return null;
                    }

                    @Override
                    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
                        return null;
                    }
                };
            }

            @Override
            protected SSLServerSocketFactory engineGetServerSocketFactory() {
                return null;
            }

            @Override
            protected SSLEngine engineCreateSSLEngine() {
                return null;
            }

            @Override
            protected SSLEngine engineCreateSSLEngine(String s, int i) {
                return null;
            }

            @Override
            protected SSLSessionContext engineGetServerSessionContext() {
                return null;
            }

            @Override
            protected SSLSessionContext engineGetClientSessionContext() {
                return null;
            }
        }, new Provider("Dummy", 1, "Dummy") {
            @Override
            public String getName() {
                return super.getName();
            }
        }, "BOGUS") {

        });
        if (proxyHost != null && !noProxyHosts.contains(this.baseURI.getHost())) {
            // Use proxy, if defined
            client = this.builderDelegate.defaultProxy(
                    proxyHost,
                    Integer.parseInt(System.getProperty("http.proxyPort", "80")))
                    .build();
        } else {
            client = this.builderDelegate.build();
        }

        actualClient = client.target(this.baseURI)
                .proxyBuilder(aClass)
                .classloader(classLoader)
                .defaultConsumes(MediaType.TEXT_PLAIN)
                .defaultProduces(MediaType.TEXT_PLAIN).build();

        Class<?>[] interfaces = new Class<?>[2];
        interfaces[0] = aClass;
        interfaces[1] = RestClientProxy.class;

        return (T) Proxy.newProxyInstance(classLoader, interfaces, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return method.invoke(actualClient, args);
            }
        });
    }

    private boolean isMapperDisabled() {
        boolean disabled = false;
        Optional<Boolean> defaultMapperProp = this.config.getOptionalValue(DEFAULT_MAPPER_PROP, Boolean.class);

        // disabled through config api
        if (defaultMapperProp.isPresent() && defaultMapperProp.get().equals(Boolean.TRUE)) {
            disabled = true;
        } else if (!defaultMapperProp.isPresent()) {

            // disabled through jaxrs property
            try {
                Object property = this.builderDelegate.getConfiguration().getProperty(DEFAULT_MAPPER_PROP);
                if (property != null) {
                    disabled = (Boolean) property;
                }
            } catch (Throwable e) {
                // ignore cast exception
            }
        }
        return disabled;
    }

    private <T> void verifyInterface(Class<T> typeDef) {

        Method[] methods = typeDef.getMethods();

        // multiple verbs
        for (Method method : methods) {
            boolean hasHttpMethod = false;
            for (Annotation annotation : method.getAnnotations()) {
                boolean isHttpMethod = (annotation.annotationType().getAnnotation(HttpMethod.class) != null);
                if (!hasHttpMethod && isHttpMethod) {
                    hasHttpMethod = true;
                } else if (hasHttpMethod && isHttpMethod) {
                    throw new RestClientDefinitionException("Ambiguous @Httpmethod defintion on type " + typeDef);
                }
            }
        }

        // invalid parameter
        Path classPathAnno = typeDef.getAnnotation(Path.class);

        final Set<String> classLevelVariables = new HashSet<>();
        ResteasyUriBuilder classTemplate = null;
        if (classPathAnno != null) {
            classTemplate = (ResteasyUriBuilder) UriBuilder.fromUri(classPathAnno.value());
            classLevelVariables.addAll(classTemplate.getPathParamNamesInDeclarationOrder());
        }
        ResteasyUriBuilder template;
        for (Method method : methods) {

            Path methodPathAnno = method.getAnnotation(Path.class);
            if (methodPathAnno != null) {
                template = classPathAnno == null ? (ResteasyUriBuilder) UriBuilder.fromUri(methodPathAnno.value())
                        : (ResteasyUriBuilder) UriBuilder.fromUri(classPathAnno.value() + "/" + methodPathAnno.value());
            } else {
                template = classTemplate;
            }
            if (template == null) {
                continue;
            }

            // it's not executed, so this can be anything - but a hostname needs to present
            template.host("localhost");

            Set<String> allVariables = new HashSet<>(template.getPathParamNamesInDeclarationOrder());
            Map<String, Object> paramMap = new HashMap<>();
            for (Parameter p : method.getParameters()) {
                PathParam pathParam = p.getAnnotation(PathParam.class);
                if (pathParam != null) {
                    paramMap.put(pathParam.value(), "foobar");
                }
            }

            if (allVariables.size() != paramMap.size()) {
                throw new RestClientDefinitionException("Parameters and variables don't match on " + typeDef + "::" + method.getName());
            }

            try {
                template.resolveTemplates(paramMap, false).build();
            } catch (IllegalArgumentException ex) {
                throw new RestClientDefinitionException("Parameter names don't match variable names on " + typeDef + "::" + method.getName(), ex);
            }

        }
    }


    @Override
    public Configuration getConfiguration() {
        return getConfigurationWrapper();
    }

    @Override
    public RestClientBuilder property(String name, Object value) {
        if (name.startsWith(RESTEASY_PROPERTY_PREFIX)) {
            // Allows to configure some of the ResteasyClientBuilder delegate properties
            String builderMethodName = name.substring(RESTEASY_PROPERTY_PREFIX.length());
            try {
                Method builderMethod = ResteasyClientBuilder.class.getMethod(builderMethodName, unwrapPrimitiveType(value));
                builderMethod.invoke(builderDelegate, value);
            } catch (NoSuchMethodException e) {
                log.warnf("ResteasyClientBuilder method %s not found", builderMethodName);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                log.errorf(e, "Unable to invoke ResteasyClientBuilder method %s", builderMethodName);
            }
        }
        this.builderDelegate.property(name, value);
        return this;
    }

    private static Class<?> unwrapPrimitiveType(Object value) {
        if (value instanceof Integer) {
            return int.class;
        } else if (value instanceof Long) {
            return long.class;
        } else if (value instanceof Boolean) {
            return boolean.class;
        }
        return value.getClass();
    }

    private static Object newInstanceOf(Class<?> clazz) {
        try {
            return clazz.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to register " + clazz, t);
        }
    }

    @Override
    public RestClientBuilder register(Class<?> aClass) {
        this.register(newInstanceOf(aClass));
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, int i) {

        this.register(newInstanceOf(aClass), i);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, Class<?>[] classes) {
        this.register(newInstanceOf(aClass), classes);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, Map<Class<?>, Integer> map) {
        this.register(newInstanceOf(aClass), map);
        return this;
    }

    @Override
    public RestClientBuilder register(Object o) {
        if (o instanceof ResponseExceptionMapper) {
            ResponseExceptionMapper mapper = (ResponseExceptionMapper) o;
            register(mapper, mapper.getPriority());
        } else if (o instanceof ParamConverterProvider) {
            register(o, Priorities.USER);
        } else {
            this.builderDelegate.register(o);
        }
        return this;
    }

    @Override
    public RestClientBuilder register(Object o, int i) {
        if (o instanceof ResponseExceptionMapper) {

            // local
            ResponseExceptionMapper mapper = (ResponseExceptionMapper) o;
            HashMap<Class<?>, Integer> contracts = new HashMap<>();
            contracts.put(ResponseExceptionMapper.class, i);
            registerLocalProviderInstance(mapper, contracts);

            // delegate
            this.builderDelegate.register(mapper, i);

        } else if (o instanceof ParamConverterProvider) {

            // local
            ParamConverterProvider converter = (ParamConverterProvider) o;
            HashMap<Class<?>, Integer> contracts = new HashMap<>();
            contracts.put(ParamConverterProvider.class, i);
            registerLocalProviderInstance(converter, contracts);

            // delegate
            this.builderDelegate.register(converter, i);

        } else {
            this.builderDelegate.register(o, i);
        }
        return this;
    }

    @Override
    public RestClientBuilder register(Object o, Class<?>[] classes) {

        // local
        for (Class<?> aClass : classes) {
            if (aClass.isAssignableFrom(ResponseExceptionMapper.class)) {
                register(o);
            }
        }

        // other
        this.builderDelegate.register(o, classes);
        return this;
    }

    @Override
    public RestClientBuilder register(Object o, Map<Class<?>, Integer> map) {


        if (o instanceof ResponseExceptionMapper) {

            //local
            ResponseExceptionMapper mapper = (ResponseExceptionMapper) o;
            HashMap<Class<?>, Integer> contracts = new HashMap<>();
            contracts.put(ResponseExceptionMapper.class, map.get(ResponseExceptionMapper.class));
            registerLocalProviderInstance(mapper, contracts);

            // other
            this.builderDelegate.register(o, map);

        } else {
            this.builderDelegate.register(o, map);
        }

        return this;
    }

    public Set<Object> getLocalProviderInstances() {
        return localProviderInstances;
    }

    public void registerLocalProviderInstance(Object provider, Map<Class<?>, Integer> contracts) {
        for (Object registered : getLocalProviderInstances()) {
            if (registered == provider) {
                log.infof("Provider already registered: %s", provider.getClass());
                return;
            }
        }

        localProviderInstances.add(provider);
        configurationWrapper.registerLocalContract(provider.getClass(), contracts);
    }

    private final ResteasyClientBuilder builderDelegate;

    private final ConfigurationWrapper configurationWrapper;

    private final Config config;

    private URI baseURI;

    private Set<Object> localProviderInstances = new HashSet<Object>();
}
