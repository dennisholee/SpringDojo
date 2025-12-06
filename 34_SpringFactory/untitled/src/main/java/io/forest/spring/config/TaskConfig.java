package io.forest.spring.config;

import io.forest.spring.app.DefaultTasksApp;
import io.forest.spring.domain.executable.Executable;
import io.forest.spring.domain.validation.TasksValidator;
import io.forest.spring.port.TasksApp;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskConfig {

//    @Bean
//    TasksValidator tasksValidator() {
//        return new TasksValidator();
//    }

//    @Bean
    TasksApp taskApp(TasksValidator tasksValidator, DefaultIntroductionAdvisor defaultExecutable) {
        ProxyFactory proxyFactory = new ProxyFactory();
        DefaultTasksApp defaultTasksApp = new DefaultTasksApp();
        proxyFactory.setTarget(defaultTasksApp);
        proxyFactory.addInterface(Executable.class);
        proxyFactory.addAdvice(tasksValidator);
        proxyFactory.addAdvisor(defaultExecutable);
        return (TasksApp) proxyFactory.getProxy();
    }

//    @Bean
//    DefaultIntroductionAdvisor defaultExecutable() {
//        return new DefaultIntroductionAdvisor(
//            new DelegatePerTargetObjectIntroductionInterceptor(DefaultExecutable.class,
//                Executable.class)
//        );
//    }
}
