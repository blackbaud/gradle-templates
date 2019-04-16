package com.blackbaud.templates.tasks

import com.blackbaud.templates.project.RestProject
import org.gradle.api.tasks.TaskAction

class AddPermissionsTask extends AbstractTemplateTask {
    AddPermissionsTask() {
        super("Adds a permissions class and infrastructure needed to support Blackbaud permissions")
    }

    @TaskAction
    void addPermissions() {
        RestProject restProject = new RestProject(openBasicProject())
        restProject.addPermissions()
    }
}
