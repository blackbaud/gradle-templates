package com.blackbaud.templates.tasks

import org.gradle.api.tasks.TaskAction

class AddEventHubsEventTask extends AbstractTemplateTask {

    AddEventHubsEventTask() {
        super("Adds an eventhubs event and random builder skeleton (options: -Pname=?, -Pinternal)")
    }

    @TaskAction
    void addApiObject() {
        String name = projectProps.getRequiredProjectProperty("name")
        boolean internal = projectProps.isPropertyDefined("internal")
        BasicProject basicProject = openBasicProject()
        EventHubsProject eventhubs = new EventHubsProject(basicProject)
        if(internal) {
            eventhubs.addInternalApiObject(name)
        } else {
            eventhubs.addExternalApiObject(name)
        }
    }

}
