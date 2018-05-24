package com.blackbaud.templates.tasks

import org.gradle.api.tasks.TaskAction


class AddEventHubsContainerTask extends AbstractTemplateTask {

    AddEventHubsContainerTask() {
        super("Add a eventhubs container and default configuration to an existing project (options: -Pname=?)")
    }

    @TaskAction
    void addEventHubsContainer() {
        String name = projectProps.getRequiredProjectProperty("name")
        BasicProject basicProject = openBasicProject()
        EventHubsProject eventhubs = new EventHubsProject(basicProject)
        eventhubs.initEventHubs(name)
    }

}
