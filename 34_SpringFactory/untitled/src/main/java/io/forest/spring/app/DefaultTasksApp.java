package io.forest.spring.app;

import io.forest.spring.port.TasksApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultTasksApp implements TasksApp {

    Logger log = LoggerFactory.getLogger(DefaultTasksApp.class);

    public void createTask() {
        System.out.println("createTask");
        log.info("user.login.success");
    }
}
