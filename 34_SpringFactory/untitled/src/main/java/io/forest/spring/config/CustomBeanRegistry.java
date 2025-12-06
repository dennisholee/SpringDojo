package io.forest.spring.config;

import io.forest.spring.app.DefaultTasksApp;
import io.forest.spring.domain.executable.DefaultExecutable;
import io.forest.spring.domain.executable.Executable;
import io.forest.spring.domain.validation.TasksValidator;
import io.forest.spring.port.TasksApp;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.PerformanceMonitorInterceptor;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.DelegatePerTargetObjectIntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.util.Arrays;

// @Component
public class CustomBeanRegistry implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    @Autowired
    ApplicationContext applicationContext;

    Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        // Register DefaultExecutable bean
        registry.registerBeanDefinition(
            "defaultExecutable",
            BeanDefinitionBuilder.genericBeanDefinition(DefaultExecutable.class).getBeanDefinition());

        // Register performanceMonitorInterceptor
        registry.registerBeanDefinition(
            "performanceMonitorInterceptor",
            BeanDefinitionBuilder.genericBeanDefinition(PerformanceMonitorInterceptor.class).getBeanDefinition());

        // Register performanceMonitorAdvisor
        registry.registerBeanDefinition(
            "performanceMonitorAdvisor",
            BeanDefinitionBuilder.genericBeanDefinition(DefaultPointcutAdvisor.class)
                .addPropertyReference("advice", "performanceMonitorInterceptor")
                .addPropertyValue("pointcut", Pointcut.TRUE)
                .addPropertyValue("order", Ordered.HIGHEST_PRECEDENCE)
                .getBeanDefinition());

        // Register xxx bean (DefaultTasksApp)
        registry.registerBeanDefinition(
            "xxxxx",
            BeanDefinitionBuilder.genericBeanDefinition(DefaultTasksApp.class)
                .getBeanDefinition());

        // Register TasksValidator as a bean
        registry.registerBeanDefinition(
            "tasksValidator",
            BeanDefinitionBuilder.genericBeanDefinition(TasksValidator.class).getBeanDefinition());

//        registry.registerBeanDefinition(
//            "i18nLoggerFactoryBean",
//            BeanDefinitionBuilder.genericBeanDefinition(I18nLoggerFactoryBean.class)
//                .addPropertyValue("messageSource", "messageSource")
//                .getBeanDefinition()
//        );

        Arrays.stream(registry.getBeanDefinitionNames()).forEach(System.out::println);

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

//        TasksApp tasksApp = beanFactory.getBean(
//            "xxx",
//            DefaultTasksApp.class);

        TasksApp tasksApp = beanFactory.getBean(
            "xxx",
            TasksApp.class);

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(tasksApp);

        proxyFactory.addAdvice(new TasksValidator());

        proxyFactory.addInterface(Executable.class);
        DefaultIntroductionAdvisor advisor = new DefaultIntroductionAdvisor(
            new DelegatePerTargetObjectIntroductionInterceptor(DefaultExecutable.class,
                Executable.class));

        proxyFactory.addAdvisor(advisor);

        beanFactory.registerSingleton("xxx_decorated", proxyFactory.getProxy());

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
