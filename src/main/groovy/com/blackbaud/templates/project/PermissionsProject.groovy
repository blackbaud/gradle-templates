package com.blackbaud.templates.project

import static com.google.common.base.CaseFormat.LOWER_CAMEL
import static com.google.common.base.CaseFormat.LOWER_HYPHEN

class PermissionsProject {
    private BasicProject basicProject

    PermissionsProject(BasicProject basicProject) {
        this.basicProject = basicProject
    }

    private String getServiceName() {
        basicProject.serviceName
    }

    private String getServicePackage() {
        basicProject.servicePackage
    }

    private String getServicePackagePath() {
        basicProject.servicePackagePath
    }

    void addPermissions() {
        checkForRestClient()
        addPermissionsToRestClient()
        addPermissionsConfig()
        addGlobalMethodSecurityConfigToCoreConfig()
        addPermissionsConfigToCoreConfig()
    }

    private void checkForRestClient() {
        basicProject.getProjectFileOrFail("rest-client")
    }

    private void addPermissionsToRestClient() {
        basicProject.applyTemplate("rest-client/src/main/java/${servicePackagePath}/permissions") {
            "${serviceName}Permissions.java" template: "/templates/springboot/rest/permissions/service-permissions.java.tmpl",
                                             packageName: servicePackage,
                                             serviceName: serviceName,
                                             servicePermissionPrefix: LOWER_HYPHEN.to(LOWER_CAMEL, basicProject.repoName)
        }
    }

    private void addPermissionsConfig() {
        basicProject.applyTemplate("src/main/java/${servicePackagePath}/core") {
            "PermissionsConfig.java" template: "/templates/springboot/rest/permissions/permissions-config.java.tmpl",
                                packageName: servicePackage,
                                serviceName: serviceName
        }
    }

    private void addGlobalMethodSecurityConfigToCoreConfig() {
        ProjectFile coreConfigClass = basicProject.findFile("CoreConfig.java")
        coreConfigClass.addConfigurationImport("com.blackbaud.security.GlobalMethodSecurityConfig")
    }

    private void addPermissionsConfigToCoreConfig() {
        ProjectFile coreConfigClass = basicProject.findFile("CoreConfig.java")
        coreConfigClass.addConfigurationImport("${servicePackage}.core.PermissionsConfig", true)
    }
}
