package io.forest.spring.utils;

import io.forest.spring.config.Components;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
public class JsonBeanDefinitionReader extends AbstractBeanDefinitionReader {

    public JsonBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    @Override
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Components components = objectMapper.readValue(
                resource.getInputStream(),
                Components.class);

            components.getComponents()
                .forEach(c -> {
                    getRegistry().registerBeanDefinition(
                        c.getBeanName(),
                        BeanDefinitionBuilder.genericBeanDefinition(
                                c.getClassName())
                            .getBeanDefinition()
                    );
                });

            int beanDefinitionsFound = components.getComponents().size();
            log.info("Total of {} bean definitions loaded from in {}",
                beanDefinitionsFound,
                resource.getFilename()
            );
            return beanDefinitionsFound;

        } catch (IOException e) {
            throw new BeanDefinitionStoreException(
                resource.getFilename(),
                "Unable to load bean definition due to %s".formatted(e.getMessage()),
                e);
        }
    }
}
