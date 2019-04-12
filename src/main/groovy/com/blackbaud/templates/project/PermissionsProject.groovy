package com.blackbaud.templates.project

class PermissionsProject {
    private BasicProject basicProject

    PermissionsProject(BasicProject basicProject) {
        this.basicProject = basicProject
    }

    void addPermissions() {
        checkForRestClient()
    }

    private void checkForRestClient() {
        basicProject.findFile("rest-client")
    }
}
