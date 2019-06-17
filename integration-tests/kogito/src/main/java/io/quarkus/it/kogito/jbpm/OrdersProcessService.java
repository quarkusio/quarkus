package io.quarkus.it.kogito.jbpm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.kie.kogito.Model;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstances;
import org.kie.kogito.process.WorkItem;

@Path("/runProcess")
public class OrdersProcessService {

    @Inject
    @Named("demo.orders")
    Process<? extends Model> orderProcess;

    @Inject
    @Named("demo.orderItems")
    Process<? extends Model> orderItemsProcess;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String testOrderProcess() {
        Model m = orderProcess.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "john");
        parameters.put("order", new Order("12345", false, 0.0));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = orderProcess.createInstance(m);
        processInstance.start();

        assert (org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE == processInstance.status());
        Model result = (Model) processInstance.variables();
        assert (result.toMap().size() == 2);
        assert (((Order) result.toMap().get("order")).getTotal() > 0);

        ProcessInstances<? extends Model> orderItemProcesses = orderItemsProcess.instances();
        assert (orderItemProcesses.values().size() == 1);

        ProcessInstance<?> childProcessInstance = orderItemProcesses.values().iterator().next();

        List<WorkItem> workItems = childProcessInstance.workItems();
        assert (workItems.size()) == 1;

        childProcessInstance.completeWorkItem(workItems.get(0).getId(), null);

        assert (org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED == childProcessInstance.status());
        assert (org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED == processInstance.status());

        // no active process instances for both orders and order items processes
        assert (orderProcess.instances().values().size() == 0);
        assert (orderItemsProcess.instances().values().size() == 0);

        return "OK";
    }
}
