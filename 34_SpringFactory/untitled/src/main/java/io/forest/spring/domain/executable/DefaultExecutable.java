package io.forest.spring.domain.executable;

public class DefaultExecutable implements Executable {

    @Override
    public void execute() {
        System.out.println("execute");
    }
}
