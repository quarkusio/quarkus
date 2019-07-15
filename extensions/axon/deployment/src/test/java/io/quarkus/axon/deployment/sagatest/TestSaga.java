package io.quarkus.axon.deployment.sagatest;

import java.util.Date;

import javax.inject.Inject;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;

public class TestSaga {

    @Inject
    CommandGateway commandGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "id")
    public void on(TestSagaStartEvent event, @Timestamp Date eventTime) {
        System.out.println("Saga start event on " + eventTime);
        commandGateway.send(new NextSagaFlowStepCommand(event.getId()));
    }

    @SagaEventHandler(associationProperty = "id")
    public void on(TestSagaIntermediateEvent event, @Timestamp Date eventTime) {
        System.out.println("Saga intermediate event on " + eventTime);
        commandGateway.send(new StopSagaFlowCommand(event.getId()));
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "id")
    public void on(TestSagaEndEvent event, @Timestamp Date eventTime) {
        System.out.println("Saga end event on " + eventTime);
    }
}
