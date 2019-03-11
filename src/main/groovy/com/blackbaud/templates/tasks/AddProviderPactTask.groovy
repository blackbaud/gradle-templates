package com.blackbaud.templates.tasks

import com.blackbaud.templates.project.BasicProject
import org.gradle.api.tasks.TaskAction

class AddProviderPactTask extends AbstractTemplateTask {

    AddProviderPactTask() {
        super("Initializes the project as a Pact provider")
    }

    @TaskAction
    void addProviderPact() {
        BasicProject basicProject = openBasicProject()
        basicProject.addProviderPact()
    }
}
