package com.example.datarouter.config.flow;

import com.example.datarouter.config.flow.model.Flow;
import com.example.datarouter.config.flow.model.FlowStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DynamicFlowRegistry is responsible for registering and managing dynamic integration flows.
 * It builds flows based on the provided Flow and FlowStep configurations and registers them in the application context.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicFlowRegistry {

    private final FlowBuilder flowBuilder;

    private final GenericApplicationContext applicationContext;

    private final Map<String, IntegrationFlow> flows = new ConcurrentHashMap<>();

    /**
     * Registers a flow by building its integration steps and adding them to the application context.
     * Each step in the flow is registered as a separate IntegrationFlow bean.
     *
     * @param flow the Flow object containing the steps to be registered
     */
    public void registerFlow(Flow flow) {
        log .info("Registering flow [name={}]", flow.getName());

        for (FlowStep step : flow.getSteps()) {
            String beanName = step.getName() + "Flow";

            if (!flows.containsKey(beanName)) {
                IntegrationFlow integrationFlow = flowBuilder.buildFlow(step, flow);
                flows.put(beanName, integrationFlow);

                log.info("Registering integration flow [name={}]", beanName);

                applicationContext.registerBean(
                    beanName,
                    IntegrationFlow.class,
                    () -> integrationFlow);
            }
        }
    }

    /**
     * Retrieves a registered flow by its name.
     *
     * @param flowName the name of the flow to retrieve
     * @return the IntegrationFlow associated with the given name, or null if not found
     */
    public IntegrationFlow getFlow(String flowName) {
        return flows.get(flowName);
    }
}
