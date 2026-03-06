package io.quarkus.kafka.streams.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;

@ExtendWith(MockitoExtension.class)
public class CdiAwareProcessorSupplierTest {

    @Mock
    ArcContainer container;

    @Mock
    InstanceHandle<TestProcessor> instanceHandle;

    @Mock
    ManagedContext requestContext;

    @Mock
    ProcessorContext<String, String> processorContext;

    MockedStatic<Arc> arcMock;

    TestProcessor testProcessor;

    @BeforeEach
    void setUp() {
        arcMock = Mockito.mockStatic(Arc.class);
        arcMock.when(Arc::container).thenReturn(container);
        testProcessor = new TestProcessor();
    }

    @AfterEach
    void tearDown() {
        arcMock.close();
    }

    @Test
    public void shouldCreateProcessorViaCdi() {
        when(container.instance(TestProcessor.class)).thenReturn(instanceHandle);
        when(instanceHandle.isAvailable()).thenReturn(true);
        when(instanceHandle.get()).thenReturn(testProcessor);

        CdiAwareProcessorSupplier<String, String, String, String> supplier = CdiAwareProcessorSupplier
                .of(TestProcessor.class);
        Processor<String, String, String, String> processor = supplier.get();

        assertThat(processor).isNotNull();
    }

    @Test
    public void shouldThrowIfBeanNotFound() {
        when(container.instance(TestProcessor.class)).thenReturn(instanceHandle);
        when(instanceHandle.isAvailable()).thenReturn(false);

        CdiAwareProcessorSupplier<String, String, String, String> supplier = CdiAwareProcessorSupplier
                .of(TestProcessor.class);

        assertThatThrownBy(supplier::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CDI bean not found");
    }

    @Test
    public void shouldActivateRequestContextOnProcess() {
        when(container.instance(TestProcessor.class)).thenReturn(instanceHandle);
        when(instanceHandle.isAvailable()).thenReturn(true);
        when(instanceHandle.get()).thenReturn(testProcessor);
        when(container.requestContext()).thenReturn(requestContext);
        when(requestContext.isActive()).thenReturn(false);

        Processor<String, String, String, String> processor = CdiAwareProcessorSupplier
                .of(TestProcessor.class).get();
        processor.process(new Record<>("key", "value", 0L));

        verify(requestContext).activate();
        verify(requestContext).terminate();
        assertThat(testProcessor.lastProcessedValue).isEqualTo("value");
    }

    @Test
    public void shouldSkipActivationIfContextAlreadyActive() {
        when(container.instance(TestProcessor.class)).thenReturn(instanceHandle);
        when(instanceHandle.isAvailable()).thenReturn(true);
        when(instanceHandle.get()).thenReturn(testProcessor);
        when(container.requestContext()).thenReturn(requestContext);
        when(requestContext.isActive()).thenReturn(true);

        Processor<String, String, String, String> processor = CdiAwareProcessorSupplier
                .of(TestProcessor.class).get();
        processor.process(new Record<>("key", "value", 0L));

        verify(requestContext, Mockito.never()).activate();
        verify(requestContext, Mockito.never()).terminate();
        assertThat(testProcessor.lastProcessedValue).isEqualTo("value");
    }

    @Test
    public void shouldDelegateInitAndClose() {
        when(container.instance(TestProcessor.class)).thenReturn(instanceHandle);
        when(instanceHandle.isAvailable()).thenReturn(true);
        when(instanceHandle.get()).thenReturn(testProcessor);

        Processor<String, String, String, String> processor = CdiAwareProcessorSupplier
                .of(TestProcessor.class).get();
        processor.init(processorContext);
        assertThat(testProcessor.initialized).isTrue();

        processor.close();
        assertThat(testProcessor.closed).isTrue();
        verify(instanceHandle).close();
    }

    static class TestProcessor implements Processor<String, String, String, String> {
        boolean initialized;
        boolean closed;
        String lastProcessedValue;

        @Override
        public void init(ProcessorContext<String, String> context) {
            initialized = true;
        }

        @Override
        public void process(Record<String, String> record) {
            lastProcessedValue = record.value();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
