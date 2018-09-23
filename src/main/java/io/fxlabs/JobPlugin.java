package io.fxlabs;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JobPlugin implements Plugin<Project> {


    @Override
    public void apply(Project project) {
        project.getTasks().create("invoke", Job.class, (task) -> { // <1>// <2>
        });
    }


}
