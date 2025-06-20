package com.example.datarouter.service.factory;

import com.example.datarouter.channel.DynamicChannelRegistry;
import com.example.datarouter.config.flow.model.FlowStep;
import com.example.datarouter.service.MessageProcessor;
import com.example.datarouter.service.base.AbstractMessageProcessor;
import com.example.datarouter.service.impl.LoggingMessageProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ServiceFactory {

    private final GenericApplicationContext applicationContext;

    private final Map<String, MessageProcessor> processors = new ConcurrentHashMap<>();

    private final DynamicChannelRegistry channelRegistry;

    public MessageProcessor createOrGetProcessor(FlowStep step) {
        return processors.computeIfAbsent(step.getComponentName(), name -> {
            MessageProcessor processor = createProcessor(step);
            registerProcessor(name, processor);

            // Bind the processor to its input channel
            if (step.getInputChannel() != null) {
                MessageChannel inputChannel = channelRegistry.getOrCreateChannel(step.getInputChannel());

                if (inputChannel instanceof DirectChannel) {
                    ((DirectChannel) inputChannel).subscribe(message -> {

                        processor.processMessage(message);
                        // If there's an output channel, forward the processed message
                        if (step.getOutputChannel() != null) {
                            MessageChannel outputChannel = channelRegistry.getOrCreateChannel(step.getOutputChannel());
                            outputChannel.send(message);
                        }
                    });
                }
            }

            return processor;
        });
    }

    private MessageProcessor createProcessor(FlowStep step) {
        switch (step.getComponentName().toLowerCase()) {
            case "loggingmessageprocessor":
                return new LoggingMessageProcessor();
            // Add more processor types here
            default:
                throw new IllegalArgumentException("Unknown processor type: " + step.getComponentName());
        }
    }

    private void registerProcessor(String name, MessageProcessor processor) {
        if (processor instanceof AbstractMessageProcessor) {
            applicationContext.registerBean(
                name,
                MessageProcessor.class,
                () -> processor
            );
        }
    }

    public MessageProcessor getProcessor(String name) {
        return processors.get(name);
    }
}
