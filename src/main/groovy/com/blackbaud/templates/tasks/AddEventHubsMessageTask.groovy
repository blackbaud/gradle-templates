package com.blackbaud.templates.tasks

import org.gradle.api.tasks.TaskAction

class AddEventHubsMessageTask extends AbstractTemplateTask {

    AddEventHubsMessageTask() {
        super("Adds a eventhubs message and random builder skeleton (options: -Pname=?)")
    }

    @TaskAction
    void addApiObject() {
        String name = projectProps.getRequiredProjectProperty("name")
        BasicProject basicProject = openBasicProject()
        EventHubsProject eventhubs = new EventHubsProject(basicProject)
        eventhubs.addApiObject(name)
    }

}
