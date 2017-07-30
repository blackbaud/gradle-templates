package com.blackbaud.templates.tasks

import org.gradle.api.tasks.TaskAction


class AddPostgresContainerTask extends AbstractTemplateTask {

    AddPostgresContainerTask() {
        super("Add a Postgres container and default configuration to an existing project (options: -Pmybatis)")
    }

    @TaskAction
    void addPostgresContainer() {
        BasicProject basicProject = openBasicProject()
        RestProject restProject = new RestProject(basicProject)
        DatasourceProject datasourceProject = new DatasourceProject(restProject)
        datasourceProject.initPostgres()
        if (projectProps.isPropertyDefined("mybatis")) {
            datasourceProject.initMybatis()
        }
    }

}
