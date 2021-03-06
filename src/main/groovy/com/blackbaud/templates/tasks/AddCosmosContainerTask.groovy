package com.blackbaud.templates.tasks

import com.blackbaud.templates.project.DatasourceProject
import com.blackbaud.templates.project.RestProject
import org.gradle.api.tasks.TaskAction


class AddCosmosContainerTask extends AbstractTemplateTask {

    AddCosmosContainerTask() {
        super("Add a Cosmo container and default configuration to an existing project")
    }

    @TaskAction
    void addCosmosContainer() {
        RestProject restProject = openRestProject()
        DatasourceProject datasourceProject = new DatasourceProject(restProject)
        datasourceProject.initCosmos()
    }

}

