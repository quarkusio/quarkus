package io.quarkus.restclient.runtime;

import static org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.PROPERTY_PROXY_HOST;
import static org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.PROPERTY_PROXY_PORT;
import static org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.PROPERTY_PROXY_SCHEME;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.BeanParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptorFactory;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.PassthroughTrustManager;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionClientEngineBuilder;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.microprofile.client.ConfigurationWrapper;
import org.jboss.resteasy.microprofile.client.DefaultMediaTypeFilter;
import org.jboss.resteasy.microprofile.client.DefaultResponseExceptionMapper;
import org.jboss.resteasy.microprofile.client.ExceptionMapping;
import org.jboss.resteasy.microprofile.client.MethodInjectionFilter;
import org.jboss.resteasy.microprofile.client.ProxyInvocationHandler;
import org.jboss.resteasy.microprofile.client.RestClientListeners;
import org.jboss.resteasy.microprofile.client.RestClientProxy;
import org.jboss.resteasy.microprofile.client.async.AsyncInterceptorRxInvokerProvider;
import org.jboss.resteasy.microprofile.client.header.ClientHeaderProviders;
import org.jboss.resteasy.microprofile.client.header.ClientHeadersRequestFilter;
import org.jboss.resteasy.microprofile.client.impl.MpClient;
import org.jboss.resteasy.microprofile.client.impl.MpClientBuilderImpl;
import org.jboss.resteasy.microprofile.client.publisher.MpPublisherMessageBodyReader;
import org.jboss.resteasy.specimpl.ResteasyUriBuilderImpl;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ResteasyUriBuilder;

import io.quarkus.restclient.NoopHostnameVerifier;
import io.quarkus.resteasy.common.runtime.QuarkusInjectorFactory;
import io.quarkus.runtime.graal.DisabledSSLContext;
import io.quarkus.runtime.ssl.SslContextConfiguration;

/**
 * This is mostly a copy from {@link org.jboss.resteasy.microprofile.client.RestClientBuilderImpl}. It is required to
 * remove the reference to org.jboss.resteasy.cdi.CdiInjectorFactory so we don't require the RESTEasy CDI dependency.
 */
public class QuarkusRestClientBuilder implements RestClientBuilder {
    private static final String RESTEASY_PROPERTY_PREFIX = "resteasy.";

    private static final String DEFAULT_MAPPER_PROP = "microprofile.rest.client.disable.default.mapper";
    private static final Logger LOGGER = Logger.getLogger(QuarkusRestClientBuilder.class);
    private static final DefaultMediaTypeFilter DEFAULT_MEDIA_TYPE_FILTER = new DefaultMediaTypeFilter();
    private static final String TLS_TRUST_ALL = "quarkus.tls.trust-all";

    public static final MethodInjectionFilter METHOD_INJECTION_FILTER = new MethodInjectionFilter();
    public static final ClientHeadersRequestFilter HEADERS_REQUEST_FILTER = new ClientHeadersRequestFilter();

    static ResteasyProviderFactory PROVIDER_FACTORY;

    public static void setProviderFactory(ResteasyProviderFactory providerFactory) {
        PROVIDER_FACTORY = providerFactory;
    }

    public QuarkusRestClientBuilder() {
        builderDelegate = new MpClientBuilderImpl();

        if (PROVIDER_FACTORY != null) {
            ResteasyProviderFactory localProviderFactory = new LocalResteasyProviderFactory(PROVIDER_FACTORY);
            if (ResteasyProviderFactory.peekInstance() != null) {
                localProviderFactory.initializeClientProviders(ResteasyProviderFactory.getInstance());
            }
            builderDelegate.providerFactory(localProviderFactory);
        }
        if (getBeanManager() != null) {
            builderDelegate.getProviderFactory()
                    .setInjectorFactory(new QuarkusInjectorFactory());
        }
        configurationWrapper = new ConfigurationWrapper(builderDelegate.getConfiguration());

        try {
            // configuration MP may not be available.
            config = ConfigProvider.getConfig();
        } catch (Throwable e) {

        }
    }

    public Configuration getConfigurationWrapper() {
        return configurationWrapper;
    }

    @Override
    public RestClientBuilder followRedirects(boolean followRedirect) {
        this.followRedirect = followRedirect;
        return this;
    }

    public boolean isFollowRedirects() {
        return this.followRedirect;
    }

    @Override
    public RestClientBuilder queryParamStyle(QueryParamStyle queryParamStyle) {
        this.queryParamStyle = queryParamStyle;
        return this;
    }

    @Override
    public RestClientBuilder proxyAddress(String host, int port) {
        if (host == null) {
            throw new IllegalArgumentException("proxyHost must not be null");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number");
        }
        this.proxyHost = host;
        this.proxyPort = port;
        return this;
    }

    @Override
    public RestClientBuilder baseUrl(URL url) {
        try {
            baseURI = url.toURI();
            return this;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public RestClientBuilder baseUri(URI uri) {
        baseURI = uri;
        return this;
    }

    @Override
    public RestClientBuilder connectTimeout(long l, TimeUnit timeUnit) {
        connectTimeout = l;
        connectTimeoutUnit = timeUnit;
        return this;
    }

    @Override
    public RestClientBuilder readTimeout(long time, TimeUnit timeUnit) {
        readTimeout = time;
        readTimeoutUnit = timeUnit;
        return this;
    }

    @Override
    public RestClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    @Override
    public RestClientBuilder trustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
        return this;
    }

    @Override
    public RestClientBuilder keyStore(KeyStore keyStore, String keystorePassword) {
        this.keyStore = keyStore;
        this.keystorePassword = keystorePassword;
        return this;
    }

    @Override
    public RestClientBuilder hostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    @Override
    public RestClientBuilder executorService(ExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("ExecutorService must not be null");
        }
        executorService = executor;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T build(Class<T> aClass) throws IllegalStateException, RestClientDefinitionException {

        RestClientListeners.get().forEach(listener -> listener.onNewClient(aClass, this));

        // Interface validity
        verifyInterface(aClass);

        if (baseURI == null) {
            throw new IllegalStateException("Neither baseUri nor baseUrl was specified");
        }

        // Provider annotations
        RegisterProvider[] providers = aClass.getAnnotationsByType(RegisterProvider.class);

        for (RegisterProvider provider : providers) {
            register(provider.value(), provider.priority());
        }

        // Default exception mapper
        if (!isMapperDisabled()) {
            register(DefaultResponseExceptionMapper.class);
        }

        builderDelegate.register(new ExceptionMapping(localProviderInstances), 1);

        ClassLoader classLoader = aClass.getClassLoader();

        T actualClient;
        ResteasyClient client;

        ResteasyClientBuilder resteasyClientBuilder;
        List<String> noProxyHosts = Arrays.asList(
                getSystemProperty("http.nonProxyHosts", "localhost|127.*|[::1]").split("\\|"));
        if (this.proxyHost != null) {
            resteasyClientBuilder = builderDelegate.defaultProxy(proxyHost, this.proxyPort);
        } else {
            String envProxyHost = getSystemProperty("http.proxyHost", null);
            boolean isUriMatched = false;
            if (envProxyHost != null && !noProxyHosts.isEmpty()) {
                for (String s : noProxyHosts) {
                    Pattern p = Pattern.compile(s);
                    Matcher m = p.matcher(baseURI.getHost());
                    isUriMatched = m.matches();
                    if (isUriMatched) {
                        break;
                    }
                }
            }

            if (envProxyHost != null && !isUriMatched) {
                // Use proxy, if defined in the env variables
                resteasyClientBuilder = builderDelegate.defaultProxy(envProxyHost,
                        Integer.parseInt(getSystemProperty("http.proxyPort", "80")));
            } else {
                // Search for proxy settings passed in the client builder, if passed and use them if found
                String userProxyHost = Optional.ofNullable(getConfiguration().getProperty(PROPERTY_PROXY_HOST))
                        .filter(String.class::isInstance).map(String.class::cast).orElse(null);

                Integer userProxyPort = Optional.ofNullable(getConfiguration().getProperty(PROPERTY_PROXY_PORT))
                        .filter(Integer.class::isInstance).map(Integer.class::cast).orElse(null);

                String userProxyScheme = Optional.ofNullable(getConfiguration().getProperty(PROPERTY_PROXY_SCHEME))
                        .filter(String.class::isInstance).map(String.class::cast).orElse(null);

                if (userProxyHost != null && userProxyPort != null) {
                    resteasyClientBuilder = builderDelegate.defaultProxy(userProxyHost, userProxyPort, userProxyScheme);
                } else {
                    // ProxySelector if applicable
                    selectHttpProxy().ifPresent(
                            proxyAddress -> builderDelegate.defaultProxy(proxyAddress.getHostString(), proxyAddress.getPort()));

                    resteasyClientBuilder = builderDelegate;
                }
            }
        }

        if (this.executorService != null) {
            resteasyClientBuilder.executorService(this.executorService);
        } else {
            this.executorService = Executors.newCachedThreadPool();
            resteasyClientBuilder.executorService(executorService, true);
        }

        resteasyClientBuilder.register(DEFAULT_MEDIA_TYPE_FILTER);
        resteasyClientBuilder.register(METHOD_INJECTION_FILTER);
        resteasyClientBuilder.register(HEADERS_REQUEST_FILTER);
        register(new MpPublisherMessageBodyReader(executorService));
        resteasyClientBuilder.sslContext(sslContext);
        resteasyClientBuilder.trustStore(trustStore);
        resteasyClientBuilder.keyStore(keyStore, keystorePassword);

        resteasyClientBuilder.hostnameVerifier(hostnameVerifier);
        resteasyClientBuilder.setIsTrustSelfSignedCertificates(false);
        checkQueryParamStyleProperty(aClass);
        checkFollowRedirectProperty(aClass);
        resteasyClientBuilder.setFollowRedirects(followRedirect);

        if (readTimeout != null) {
            resteasyClientBuilder.readTimeout(readTimeout, readTimeoutUnit);
        }
        if (connectTimeout != null) {
            resteasyClientBuilder.connectTimeout(connectTimeout, connectTimeoutUnit);
        }

        if (useURLConnection()) {
            resteasyClientBuilder
                    .httpEngine(new URLConnectionClientEngineBuilder().resteasyClientBuilder(resteasyClientBuilder).build());
            resteasyClientBuilder.sslContext(null);
            resteasyClientBuilder.trustStore(null);
            resteasyClientBuilder.keyStore(null, "");
        }

        configureTrustAll(resteasyClientBuilder);
        // we need to push a disabled SSL context when SSL has been disabled
        // because otherwise Apache HTTP Client will try to initialize one and will fail
        if (ImageInfo.inImageRuntimeCode() && !SslContextConfiguration.isSslNativeEnabled()) {
            resteasyClientBuilder.sslContext(new DisabledSSLContext());
        }

        client = resteasyClientBuilder
                .build();
        ((MpClient) client).setQueryParamStyle(queryParamStyle);
        client.register(AsyncInterceptorRxInvokerProvider.class);
        actualClient = client.target(baseURI)
                .proxyBuilder(aClass)
                .classloader(classLoader)
                .defaultConsumes(MediaType.APPLICATION_JSON)
                .defaultProduces(MediaType.APPLICATION_JSON).build();

        Class<?>[] interfaces = new Class<?>[3];
        interfaces[0] = aClass;
        interfaces[1] = RestClientProxy.class;
        interfaces[2] = Closeable.class;

        T proxy = (T) Proxy.newProxyInstance(classLoader, interfaces,
                new ProxyInvocationHandler(aClass, actualClient, getLocalProviderInstances(), client, getBeanManager()));
        ClientHeaderProviders.registerForClass(aClass, proxy, getBeanManager());
        return proxy;
    }

    private void configureTrustAll(ResteasyClientBuilder clientBuilder) {
        if (config == null) {
            return;
        }
        Optional<Boolean> trustAll = config.getOptionalValue(TLS_TRUST_ALL, Boolean.class);
        if (trustAll.isPresent() && trustAll.get()) {
            clientBuilder.hostnameVerifier(new NoopHostnameVerifier());
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[] { new PassthroughTrustManager() },
                        new SecureRandom());
                clientBuilder.sslContext(sslContext);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException("Failed to initialized SSL context", e);
            }
        }
    }

    /**
     * Determines whether or not to default to using the URLConnection instead of the Apache HTTP Client.
     * If the {@code org.jboss.resteasy.microprofile.defaultToURLConnectionHttpClient} system property is {@code true},
     * then this method returns {@code true}. In all other cases it returns {@code false}
     */
    private boolean useURLConnection() {
        if (useURLConnection == null) {
            String defaultToURLConnection = getSystemProperty(
                    "org.jboss.resteasy.microprofile.defaultToURLConnectionHttpClient", "false");
            useURLConnection = defaultToURLConnection.toLowerCase().equals("true");
        }
        return useURLConnection;
    }

    private Optional<InetSocketAddress> selectHttpProxy() {
        return ProxySelector.getDefault().select(baseURI).stream()
                .filter(proxy -> proxy.type() == java.net.Proxy.Type.HTTP)
                .map(java.net.Proxy::address)
                .map(InetSocketAddress.class::cast)
                .findFirst();
    }

    private void checkQueryParamStyleProperty(Class aClass) {
        // User's programmatic setting takes precedence over
        // microprofile-config.properties.
        if (queryParamStyle == null) {
            if (config != null) {
                // property using fully-qualified class name takes precedence
                Optional<String> prop = config.getOptionalValue(
                        aClass.getName() + "/mp-rest/queryParamStyle", String.class);
                if (prop.isPresent()) {
                    queryParamStyle(QueryParamStyle.valueOf(
                            prop.get().trim().toUpperCase()));

                } else {
                    RegisterRestClient registerRestClient = (RegisterRestClient) aClass.getAnnotation(RegisterRestClient.class);
                    if (registerRestClient != null &&
                            registerRestClient.configKey() != null &&
                            !registerRestClient.configKey().isEmpty()) {

                        //property using configKey
                        prop = config.getOptionalValue(registerRestClient.configKey()
                                + "/mp-rest/queryParamStyle", String.class);
                        if (prop.isPresent()) {
                            queryParamStyle(QueryParamStyle.valueOf(
                                    prop.get().trim().toUpperCase()));
                        }
                    }
                }
            }
        }
        if (queryParamStyle == null) {
            queryParamStyle = QueryParamStyle.MULTI_PAIRS;
        }
    }

    private void checkFollowRedirectProperty(Class aClass) {
        // User's programmatic setting takes precedence over
        // microprofile-config.properties.
        if (!followRedirect) {
            if (config != null) {
                // property using fully-qualified class name takes precedence
                Optional<Boolean> prop = config.getOptionalValue(
                        aClass.getName() + "/mp-rest/followRedirects", Boolean.class);
                if (prop.isPresent()) {
                    if (prop.get() != followRedirect) {
                        followRedirects(prop.get());
                    }
                } else {
                    RegisterRestClient registerRestClient = (RegisterRestClient) aClass.getAnnotation(RegisterRestClient.class);
                    if (registerRestClient != null &&
                            registerRestClient.configKey() != null &&
                            !registerRestClient.configKey().isEmpty()) {

                        //property using configKey
                        prop = config.getOptionalValue(
                                registerRestClient.configKey() + "/mp-rest/followRedirects", Boolean.class);
                        if (prop.isPresent()) {
                            if (prop.get() != followRedirect) {
                                followRedirects(prop.get());
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isMapperDisabled() {
        boolean disabled = false;
        Optional<Boolean> defaultMapperProp = config == null ? Optional.empty()
                : config.getOptionalValue(DEFAULT_MAPPER_PROP, Boolean.class);

        // disabled through config api
        if (defaultMapperProp.isPresent() && defaultMapperProp.get().equals(Boolean.TRUE)) {
            disabled = true;
        } else if (!defaultMapperProp.isPresent()) {

            // disabled through jaxrs property
            try {
                Object property = builderDelegate.getConfiguration().getProperty(DEFAULT_MAPPER_PROP);
                if (property != null) {
                    disabled = (Boolean) property;
                }
            } catch (Throwable e) {
                // ignore cast exception
            }
        }
        return disabled;
    }

    private String getReflectName(AnnotatedElement element) {
        if (element instanceof Parameter) {
            return ((Parameter) element).getName();
        } else if (element instanceof Field) {
            return ((Field) element).getName();
        } else if (element instanceof Method) {
            Method m = (Method) element;
            if (!m.getName().startsWith("get"))
                return null;
            return Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
        }
        return null;
    }

    private String getPathParamName(AnnotatedElement element) {
        if (element.isAnnotationPresent(PathParam.class)) {
            PathParam pp = element.getAnnotation(PathParam.class);
            return pp.value();
        } else if (element.isAnnotationPresent(org.jboss.resteasy.annotations.jaxrs.PathParam.class)) {
            org.jboss.resteasy.annotations.jaxrs.PathParam pp = element
                    .getAnnotation(org.jboss.resteasy.annotations.jaxrs.PathParam.class);
            if (pp.value().length() > 0)
                return pp.value();
            return getReflectName(element);
        }
        return null;
    }

    private void verifyBeanPathParam(Class<?> beanType, Map<String, Object> paramMap) {
        for (Field field : beanType.getDeclaredFields()) {
            String name = getPathParamName(field);
            if (name != null) {
                paramMap.put(name, "foobar");
            }
        }

        for (Method m : beanType.getDeclaredMethods()) {
            String name = getPathParamName(m);
            if (name != null) {
                paramMap.put(name, "foobar");
            }

        }
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

        ResteasyUriBuilder template = null;
        for (Method method : methods) {
            Path methodPathAnno = method.getAnnotation(Path.class);
            if (methodPathAnno != null) {
                template = classPathAnno == null ? (ResteasyUriBuilder) new ResteasyUriBuilderImpl().uri(methodPathAnno.value())
                        : (ResteasyUriBuilder) new ResteasyUriBuilderImpl()
                                .uri(classPathAnno.value() + "/" + methodPathAnno.value());
            } else if (classPathAnno != null) {
                template = (ResteasyUriBuilder) new ResteasyUriBuilderImpl().uri(classPathAnno.value());
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
                } else if (p.isAnnotationPresent(org.jboss.resteasy.annotations.jaxrs.PathParam.class)) {
                    org.jboss.resteasy.annotations.jaxrs.PathParam rePathParam = p
                            .getAnnotation(org.jboss.resteasy.annotations.jaxrs.PathParam.class);
                    String name = rePathParam.value() == null || rePathParam.value().length() == 0 ? p.getName()
                            : rePathParam.value();
                    paramMap.put(name, "foobar");
                } else if (p.isAnnotationPresent(BeanParam.class)) {
                    verifyBeanPathParam(p.getType(), paramMap);
                }
            }

            if (allVariables.size() != paramMap.size()) {
                throw new RestClientDefinitionException(
                        "Parameters and variables don't match on " + typeDef + "::" + method.getName());
            }

            try {
                template.resolveTemplates(paramMap, false).build();
            } catch (IllegalArgumentException ex) {
                throw new RestClientDefinitionException(
                        "Parameter names don't match variable names on " + typeDef + "::" + method.getName(), ex);
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
            // Makes it possible to configure some of the ResteasyClientBuilder delegate properties
            String builderMethodName = name.substring(RESTEASY_PROPERTY_PREFIX.length());
            Method builderMethod = Arrays.stream(ResteasyClientBuilder.class.getMethods())
                    .filter(m -> builderMethodName.equals(m.getName()) && m.getParameterTypes().length >= 1)
                    .findFirst()
                    .orElse(null);
            if (builderMethod == null) {
                throw new IllegalArgumentException("ResteasyClientBuilder setter method not found: " + builderMethodName);
            }
            Object[] arguments;
            if (builderMethod.getParameterTypes().length > 1) {
                if (value instanceof List) {
                    arguments = ((List<?>) value).toArray();
                } else {
                    throw new IllegalArgumentException(
                            "Value must be an instance of List<> for ResteasyClientBuilder setter method: "
                                    + builderMethodName);
                }
            } else {
                arguments = new Object[] { value };
            }
            try {
                builderMethod.invoke(builderDelegate, arguments);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new IllegalStateException("Unable to invoke ResteasyClientBuilder method: " + builderMethodName, e);
            }
        }
        builderDelegate.property(name, value);
        return this;
    }

    private Object newInstanceOf(Class<?> clazz) {
        if (PROVIDER_FACTORY != null) {
            return PROVIDER_FACTORY.injectedInstance(clazz);
        }
        return this.getBuilderDelegate().getProviderFactory().injectedInstance(clazz);
    }

    @Override
    public RestClientBuilder register(Class<?> aClass) {
        register(newInstanceOf(aClass));
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, int i) {

        register(newInstanceOf(aClass), i);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, Class<?>[] classes) {
        register(newInstanceOf(aClass), classes);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, Map<Class<?>, Integer> map) {
        register(newInstanceOf(aClass), map);
        return this;
    }

    @Override
    public RestClientBuilder register(Object o) {
        if (o instanceof ResponseExceptionMapper) {
            ResponseExceptionMapper mapper = (ResponseExceptionMapper) o;
            register(mapper, mapper.getPriority());
        } else if (o instanceof ParamConverterProvider) {
            register(o, Priorities.USER);
        } else if (o instanceof AsyncInvocationInterceptorFactory) {
            builderDelegate.asyncInterceptorFactories.add((AsyncInvocationInterceptorFactory) o);
        } else {
            builderDelegate.register(o);
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
            builderDelegate.register(mapper, i);

        } else if (o instanceof ParamConverterProvider) {

            // local
            ParamConverterProvider converter = (ParamConverterProvider) o;
            HashMap<Class<?>, Integer> contracts = new HashMap<>();
            contracts.put(ParamConverterProvider.class, i);
            registerLocalProviderInstance(converter, contracts);

            // delegate
            builderDelegate.register(converter, i);

        } else if (o instanceof AsyncInvocationInterceptorFactory) {
            builderDelegate.asyncInterceptorFactories.add((AsyncInvocationInterceptorFactory) o);
        } else {
            builderDelegate.register(o, i);
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
        builderDelegate.register(o, classes);
        return this;
    }

    @Override
    public RestClientBuilder register(Object o, Map<Class<?>, Integer> map) {

        if (o instanceof ResponseExceptionMapper) {

            // local
            ResponseExceptionMapper mapper = (ResponseExceptionMapper) o;
            HashMap<Class<?>, Integer> contracts = new HashMap<>();
            contracts.put(ResponseExceptionMapper.class, map.get(ResponseExceptionMapper.class));
            registerLocalProviderInstance(mapper, contracts);

            // other
            builderDelegate.register(o, map);

        } else {
            builderDelegate.register(o, map);
        }

        return this;
    }

    public Set<Object> getLocalProviderInstances() {
        return localProviderInstances;
    }

    public void registerLocalProviderInstance(Object provider, Map<Class<?>, Integer> contracts) {
        for (Object registered : getLocalProviderInstances()) {
            if (registered == provider) {
                // System.out.println("Provider already registered " + provider.getClass().getName());
                return;
            }
        }

        localProviderInstances.add(provider);
        configurationWrapper.registerLocalContract(provider.getClass(), contracts);
    }

    ResteasyClientBuilder getBuilderDelegate() {
        return builderDelegate;
    }

    private static BeanManager getBeanManager() {
        try {
            CDI<Object> current = CDI.current();
            return current != null ? current.getBeanManager() : null;
        } catch (IllegalStateException e) {
            LOGGER.warnf("CDI container is not available");
            return null;
        }
    }

    private String getSystemProperty(String key, String def) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(key, def);
        }
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, def));
    }

    private final MpClientBuilderImpl builderDelegate;

    private final ConfigurationWrapper configurationWrapper;

    private Config config;

    private ExecutorService executorService;

    private URI baseURI;

    private Long connectTimeout;
    private TimeUnit connectTimeoutUnit;

    private Long readTimeout;
    private TimeUnit readTimeoutUnit;

    private String proxyHost;
    private Integer proxyPort = null;

    private SSLContext sslContext;
    private KeyStore trustStore;
    private KeyStore keyStore;
    private String keystorePassword;
    private HostnameVerifier hostnameVerifier;
    private Boolean useURLConnection;
    private boolean followRedirect;
    private QueryParamStyle queryParamStyle = null;

    private Set<Object> localProviderInstances = new HashSet<>();
}
