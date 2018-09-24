package io.fxlabs.job;

import io.fxlabs.job.Job;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JobPlugin implements Plugin<Project> {


    @Override
    public void apply(Project project) {
        project.getTasks().create("fxjob", Job.class, (task) -> {
            // <1>// <2>

            task.setHost("https://cloud.io.fxlabs.io");


        });
    }


}
