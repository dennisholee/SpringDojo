package io.forest.spring;

import io.forest.spring.app.DefaultTasksApp;
import io.forest.spring.domain.executable.Executable;
import io.forest.spring.utils.JsonBeanDefinitionReader;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.support.GenericApplicationContext;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
//        ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);
//
////        TasksApp taskApp = (TasksApp) applicationContext.getBean("taskApp");
////
////        taskApp.createTask();
////        ((Executable) taskApp).execute();
////
//       BeanDefinition beanDefinition = applicationContext.getBeanFactory()
//            .getBeanDefinition("xxxxx");
//
//        System.out.println(beanDefinition);
//
//        DefaultTasksApp taskApp = (DefaultTasksApp) applicationContext.getBean("xxx_decorated");
//        System.out.println("-------");
//        taskApp.createTask();
//        ((Executable) taskApp).execute();

        GenericApplicationContext genericApplicationContext = new GenericApplicationContext();
        JsonBeanDefinitionReader jsonBeanDefinitionReader = new JsonBeanDefinitionReader(genericApplicationContext);
        jsonBeanDefinitionReader.loadBeanDefinitions("classpath:components.json");


        genericApplicationContext.refresh();
        DefaultTasksApp taskApp = (DefaultTasksApp) genericApplicationContext.getBean("xxx_decorated");

        System.out.println("-------");
        taskApp.createTask();

        ((Executable) taskApp).execute();




    }
}
