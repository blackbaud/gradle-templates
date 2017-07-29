package com.blackbaud.templates.tasks

import org.gradle.api.tasks.TaskAction

class CreateBasicResourceTask extends AbstractTemplateTask {

    CreateBasicResourceTask() {
        super("Create a SpringBoot REST resource (options: -PresourceName=?)")
    }

    @TaskAction
    void createBasicResource() {
        String resourceName = getResourceName()
        RestProject restProject = openRestProject()
        restProject.createBasicResource(resourceName)
    }

    private String getResourceName() {
        String resourceName = projectProps.getRequiredProjectProperty("resourceName")
        "${resourceName.capitalize()}"
    }

}
