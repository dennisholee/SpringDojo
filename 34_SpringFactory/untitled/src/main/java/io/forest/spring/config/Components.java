package io.forest.spring.config;

import lombok.Data;

import java.util.List;

@Data
public class Components {

    private List<ComponentDescriptor> components;

    @Data
    public static class ComponentDescriptor {
        private String beanName;
        private String className;
    }
}

