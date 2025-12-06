package io.forest.spring.domain.validation;

import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

public class TasksValidator implements MethodBeforeAdvice {
    @Override
    public void before(Method method,
                       Object[] args,
                       Object target) throws Throwable {

        switch(method.getName()) {
            case "createTask": {
                System.out.println("Validation");
            }
        }

    }
}
