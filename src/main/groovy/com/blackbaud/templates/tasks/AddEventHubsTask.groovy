package com.blackbaud.templates.tasks

import com.blackbaud.templates.project.BasicProject
import com.blackbaud.templates.project.EventHubsProject
import org.gradle.api.tasks.TaskAction


class AddEventHubsTask extends AbstractTemplateTask {

    AddEventHubsTask() {
        super("Add an eventhubs and default configuration to an existing project (options: -Pname=?)")
    }

    @TaskAction
    void addEventHubsContainer() {
        String name = projectProps.getRequiredProjectProperty("name")
        BasicProject basicProject = openBasicProject()
        EventHubsProject eventhubs = new EventHubsProject(basicProject)
        eventhubs.initEventHubsIfNotAlreadyInitialized(name)
    }

}
