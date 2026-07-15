package io.quarkus.tck.rest;

import static io.quarkus.tck.rest.DisableReason.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class QuarkusRestTckDisabledTests implements ExecutionCondition, BeforeEachCallback {

    private static final String TCK_PREFIX = "ee.jakarta.tck.ws.rs.";

    private static final Map<String, DisableReason> DISABLED_CLASSES = new HashMap<>();
    private static final Map<String, DisableReason> DISABLED_METHODS = new HashMap<>();

    static {
        // =====================================================================
        // Class-level exclusions
        // =====================================================================

        disableClass("signaturetest.jaxrs.JAXRSSigTestIT", SIGNATURE_TEST);

        disableClass("spec.context.server.JAXRSClientIT", UNSUPPORTED_APPLICATION_SINGLETONS);

        disableClass("ee.rs.cookieparam.locator.JAXRSLocatorClientIT", LOCATOR_ISSUES);
        disableClass("ee.rs.formparam.locator.JAXRSLocatorClientIT", LOCATOR_ISSUES);
        disableClass("ee.rs.headerparam.locator.JAXRSLocatorClientIT", LOCATOR_ISSUES);
        disableClass("ee.rs.matrixparam.locator.JAXRSLocatorClientIT", LOCATOR_ISSUES);
        disableClass("ee.rs.pathparam.locator.JAXRSLocatorClientIT", LOCATOR_ISSUES);

        // =====================================================================
        // Method-level exclusions: XML/JAXB providers
        // =====================================================================

        disable("spec.provider.jaxbcontext.JAXRSClientIT", "readWriteProviderTest", UNSUPPORTED_XML);

        disable("spec.provider.standardhaspriority.JAXRSClientIT", "readWriteJaxbProviderTest", UNSUPPORTED_XML);

        disable("spec.provider.standard.JAXRSClientIT", "sourceProviderTest", UNSUPPORTED_XML);

        disable("spec.provider.standardnotnull.JAXRSClientIT", "serverJaxbProviderTest", UNSUPPORTED_XML);
        disable("spec.provider.standardnotnull.JAXRSClientIT", "clientJaxbProviderTest", UNSUPPORTED_XML);

        disable("spec.provider.standardwithxmlbinding.JAXRSClientIT", "jaxbElementProviderTest", UNSUPPORTED_XML);

        disable("spec.client.typedentitieswithxmlbinding.JAXRSClientIT", "clientJaxbElementWriterTest", UNSUPPORTED_XML);
        disable("spec.client.typedentitieswithxmlbinding.JAXRSClientIT", "clientJaxbElementReaderTest", UNSUPPORTED_XML);

        disable("spec.filter.interceptor.JAXRSClientIT", "jaxbReaderContainerInterceptorTest", UNSUPPORTED_XML);
        disable("spec.filter.interceptor.JAXRSClientIT", "jaxbReaderNoInterceptorTest", UNSUPPORTED_XML);

        disable("spec.resource.requestmatching.JAXRSClientIT", "consumesOnResourceLocatorTest", UNSUPPORTED_XML);
        disable("spec.resource.requestmatching.JAXRSClientIT", "consumesOnSubResourceLocatorTest", UNSUPPORTED_XML);
        disable("spec.resource.requestmatching.JAXRSClientIT", "producesOverridesDescendantSubResourcePathValueWeightTest",
                UNSUPPORTED_XML);

        disable("jaxrs21.ee.sse.sseeventsource.JAXRSClientIT", "xmlTest", UNSUPPORTED_XML);
        disable("jaxrs21.ee.sse.sseeventsource.JAXRSClientIT", "jaxbElementTest", UNSUPPORTED_XML);

        // =====================================================================
        // Method-level exclusions: DataSource
        // =====================================================================

        disable("spec.provider.standard.JAXRSClientIT", "dataSourceProviderTest", UNSUPPORTED_DATASOURCE);

        disable("spec.provider.standardnotnull.JAXRSClientIT", "serverDataSourceProviderTest", UNSUPPORTED_DATASOURCE);
        disable("spec.provider.standardnotnull.JAXRSClientIT", "clientDataSourceProviderTest", UNSUPPORTED_DATASOURCE);

        disable("spec.client.typedentities.JAXRSClientIT", "clientDataSourceReaderTest", UNSUPPORTED_DATASOURCE);
        disable("spec.client.typedentities.JAXRSClientIT", "clientDataSourceWriterTest", UNSUPPORTED_DATASOURCE);

        disable("spec.filter.interceptor.JAXRSClientIT", "dataSourceReaderContainerInterceptorTest", UNSUPPORTED_DATASOURCE);
        disable("spec.filter.interceptor.JAXRSClientIT", "dataSourceReaderNoInterceptorTest", UNSUPPORTED_DATASOURCE);
        disable("spec.filter.interceptor.JAXRSClientIT", "dataSourceWriterClientInterceptorTest", UNSUPPORTED_DATASOURCE);

        disable("jaxrs21.ee.sse.sseeventsink.JAXRSClientIT", "datasourceTest", UNSUPPORTED_DATASOURCE);
        disable("jaxrs21.ee.sse.sseeventsource.JAXRSClientIT", "dataSourceTest", UNSUPPORTED_DATASOURCE);

        // =====================================================================
        // Method-level exclusions: Source
        // =====================================================================

        disable("spec.client.typedentities.JAXRSClientIT", "clientSourceReaderTest", UNSUPPORTED_SOURCE);
        disable("spec.client.typedentities.JAXRSClientIT", "clientSourceWriterTest", UNSUPPORTED_SOURCE);

        disable("spec.provider.standardnotnull.JAXRSClientIT", "clientSourceProviderTest", UNSUPPORTED_SOURCE);

        disable("spec.filter.interceptor.JAXRSClientIT", "sourceWriterNoInterceptorTest", UNSUPPORTED_SOURCE);

        disable("jaxrs21.ee.sse.sseeventsource.JAXRSClientIT", "transformSourceTest", UNSUPPORTED_SOURCE);

        // =====================================================================
        // Method-level exclusions: StreamingOutput
        // =====================================================================

        disable("spec.client.typedentities.JAXRSClientIT", "clientStreamingOutputWriterTest", UNSUPPORTED_STREAMING_OUTPUT);

        // =====================================================================
        // Method-level exclusions: File handling
        // =====================================================================

        disable("spec.filter.interceptor.JAXRSClientIT", "fileWriterClientInterceptorTest", FILE_HANDLING);

        // =====================================================================
        // Method-level exclusions: Client/Server injection separation
        // =====================================================================

        disable("spec.context.client.JAXRSClientIT", "clientWriterTest", UNSUPPORTED_CLIENT_SERVER_INJECTION_SEPARATION);
        disable("spec.context.client.JAXRSClientIT", "clientReaderTest", UNSUPPORTED_CLIENT_SERVER_INJECTION_SEPARATION);

        // =====================================================================
        // Method-level exclusions: Underspecified / Unsupported behavior
        // =====================================================================

        disable("spec.client.typedentities.JAXRSClientIT", "clientAnyWriterUsageTest", UNDERSPECIFIED);

        disable("api.rs.ext.runtimedelegate.create.JAXRSClientIT",
                "createEndpointThrowsIllegalArgumentExceptionTest", UNSUPPORTED);

        disable("ee.rs.ext.interceptor.clientwriter.writerinterceptorcontext.JAXRSClientIT",
                "proceedThrowsWebApplicationExceptionTest", UNDERSPECIFIED);

        disable("ee.rs.client.clientrequestcontext.JAXRSClientIT",
                "getEntityStreamTest", NOT_IMPLEMENTED_YET);

        // =====================================================================
        // Method-level exclusions: @Encoded
        // =====================================================================

        disable("ee.rs.core.uriinfo.JAXRSClientIT", "queryTest2", ENCODED);
        disable("ee.rs.core.uriinfo.JAXRSClientIT", "pathTest2", ENCODED);
        disable("ee.rs.core.uriinfo.JAXRSClientIT", "pathSegTest2", ENCODED);
        disable("ee.rs.core.uriinfo.JAXRSClientIT", "pathParamTest2", ENCODED);
        disable("ee.rs.core.uriinfo.JAXRSClientIT", "getMatchedURIsTest2", ENCODED);

        // =====================================================================
        // Method-level exclusions: Path param
        // =====================================================================

        disable("ee.rs.pathparam.JAXRSClientIT", "test6", NUTS);
        disable("ee.rs.pathparam.JAXRSClientIT", "test7", UNSUPPORTED_PATH_SEGMENT_PARAMETER_WITH_MATRIX_PARAMS);

        // =====================================================================
        // Method-level exclusions: Security context
        // =====================================================================

        disable("ee.rs.container.requestcontext.security.JAXRSClientIT", "noSecurityTest", TEST_DOESNT_MAKE_SENSE);

        // =====================================================================
        // Method-level exclusions: Sub-resource locator
        // =====================================================================

        disable("ee.rs.formparam.sub.JAXRSSubClientIT", "formParamEntityWithEncodedTest", TEST_DOESNT_MAKE_SENSE);

        disable("ee.rs.pathparam.sub.JAXRSSubClientIT", "test6", NUTS);
        disable("ee.rs.pathparam.sub.JAXRSSubClientIT", "test7", UNSUPPORTED_PATH_SEGMENT_PARAMETER_WITH_MATRIX_PARAMS);
        disable("ee.rs.pathparam.sub.JAXRSSubClientIT", "pathParamEntityWithFromStringTest", LOCATOR_ISSUES);
        disable("ee.rs.pathparam.sub.JAXRSSubClientIT", "pathFieldParamEntityWithConstructorTest",
                UNSUPPORTED_INJECTION_OF_PATH_PARAM_BEFORE_RESOURCE_LOCATOR_IS_KNOWN);
        disable("ee.rs.pathparam.sub.JAXRSSubClientIT", "pathFieldParamEntityWithValueOfTest",
                UNSUPPORTED_INJECTION_OF_PATH_PARAM_BEFORE_RESOURCE_LOCATOR_IS_KNOWN);
        disable("ee.rs.pathparam.sub.JAXRSSubClientIT", "pathFieldParamEntityWithFromStringTest",
                UNSUPPORTED_INJECTION_OF_PATH_PARAM_BEFORE_RESOURCE_LOCATOR_IS_KNOWN);
        disable("ee.rs.pathparam.sub.JAXRSSubClientIT", "pathFieldParamSetEntityWithFromStringTest",
                UNSUPPORTED_INJECTION_OF_PATH_PARAM_BEFORE_RESOURCE_LOCATOR_IS_KNOWN);
        disable("ee.rs.pathparam.sub.JAXRSSubClientIT", "pathFieldParamSortedSetEntityWithFromStringTest",
                UNSUPPORTED_INJECTION_OF_PATH_PARAM_BEFORE_RESOURCE_LOCATOR_IS_KNOWN);
        disable("ee.rs.pathparam.sub.JAXRSSubClientIT", "pathFieldParamListEntityWithFromStringTest",
                UNSUPPORTED_INJECTION_OF_PATH_PARAM_BEFORE_RESOURCE_LOCATOR_IS_KNOWN);

        // =====================================================================
        // Method-level exclusions: Threading model
        // =====================================================================

        disable("jaxrs21.ee.client.executor.rx.JAXRSClientIT", "deleteTest", THREADING_MODEL);
        disable("jaxrs21.ee.client.executor.rx.JAXRSClientIT", "optionsTest", THREADING_MODEL);

        // =====================================================================
        // Method-level exclusions: Empty query param values treated as null
        // =====================================================================

        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieParamEntityWithConstructorTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieParamEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieParamEntityWithValueOfTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieParamListEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieParamSetEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieParamSortedSetEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieFieldParamEntityWithConstructorTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieFieldParamEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieFieldParamEntityWithValueOfTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieFieldParamListEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieFieldParamSetEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.JAXRSClientIT", "cookieFieldSortedSetEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);

        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieParamEntityWithConstructorTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieParamEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieParamEntityWithValueOfTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieParamListEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieParamSetEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieParamSortedSetEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieFieldParamEntityWithConstructorTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieFieldParamEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieFieldParamEntityWithValueOfTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieFieldParamListEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieFieldParamSetEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);
        disable("ee.rs.cookieparam.sub.JAXRSSubClientIT", "cookieFieldSortedSetEntityWithFromStringTest", EMPTY_PARAM_IS_NULL);

        // =====================================================================
        // Method-level exclusions: Client exception wrapping
        // =====================================================================

        disable("spec.client.exceptions.ClientExceptionsIT", "shouldThrowMostSpecificWebApplicationException",
                CLIENT_EXCEPTION_WRAPPING);
    }

    private static void disableClass(String shortClassName, DisableReason reason) {
        DISABLED_CLASSES.put(TCK_PREFIX + shortClassName, reason);
    }

    private static void disable(String shortClassName, String methodName, DisableReason reason) {
        DISABLED_METHODS.put(TCK_PREFIX + shortClassName + "#" + methodName, reason);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        HeaderUtil.clearHeaderDelegateCache();
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String className = context.getRequiredTestClass().getName();

        DisableReason classReason = DISABLED_CLASSES.get(className);
        if (classReason != null) {
            return ConditionEvaluationResult.disabled(classReason.description());
        }

        if (context.getTestMethod().isPresent()) {
            Method method = context.getTestMethod().get();
            String key = className + "#" + method.getName();
            DisableReason methodReason = DISABLED_METHODS.get(key);
            if (methodReason != null) {
                return ConditionEvaluationResult.disabled(methodReason.description());
            }
        }

        return ConditionEvaluationResult.enabled("Not disabled");
    }
}
