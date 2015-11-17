package templates.tasks

import org.gradle.api.GradleException

import static com.google.common.base.CaseFormat.LOWER_HYPHEN
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE
import static com.google.common.base.CaseFormat.UPPER_CAMEL

class RestProject {

    private BasicProject basicProject
    private String serviceName
    private String servicePackage
    private String servicePackagePath

    RestProject(BasicProject basicProject) {
        this.basicProject = basicProject
        serviceName = LOWER_HYPHEN.to(UPPER_CAMEL, basicProject.repoName)
        servicePackage = "com.blackbaud.${serviceName.toLowerCase()}"
        servicePackagePath = servicePackage.replaceAll("\\.", "/")
    }

    void initRestProject() {
        basicProject.initGradleProject()
        createRestBase()
    }

    private void createRestBase() {
        basicProject.applyTemplate("src/main/java/${servicePackagePath}") {
            "${serviceName}.java" template: "/templates/springboot/application-class.java.tmpl",
                    serviceName: serviceName, servicePackage: servicePackage

            'api' {
                'ResourcePaths.java' template: "/templates/springboot/resource-paths.java.tmpl",
                        packageName: "${servicePackage}.api"
            }
        }

        basicProject.applyTemplate("src/componentTest/java/${servicePackagePath}") {
            "ComponentTest.java" template: "/templates/springboot/component-test-annotation.java.tmpl",
                    serviceName: serviceName, packageName: servicePackage

            "${serviceName}TestConfig.java" template: "/templates/springboot/application-test-config.java.tmpl",
                    className: "${serviceName}TestConfig", packageName: servicePackage
        }

        basicProject.applyTemplate("src/mainTest/groovy/${servicePackagePath}") {
            "ARandom.java" template: "/templates/springboot/arandom.java.tmpl",
                    packageName: servicePackage
        }

        basicProject.applyTemplate {
            'build.gradle' template: "/templates/springboot/build.gradle.tmpl"
            'src' {
                'main' {
                    'resources' {
                        'application.properties' template: "/templates/springboot/application.properties.tmpl"
                    }
                }
                'test' {
                    "groovy" {}
                }
                'componentTest' {
                    'resources' {
                        'logback.xml' template: "/templates/logback/logback.tmpl"

                        'db' {
                            "test_cleanup.sql" content: ""
                        }
                    }
                }
            }
        }

        basicProject.commitProjectFiles("springboot rest bootstrap")
    }

    void createRestResource(String resourceName) {
        String resourcePath = "${UPPER_CAMEL.to(LOWER_UNDERSCORE, resourceName)}"
        String resourceVarName = "${resourcePath.toUpperCase()}_PATH"
        basicProject.applyTemplate("src/main/java/${servicePackagePath}/resources") {
            "${resourceName}Resource.java" template: "/templates/springboot/rest-resource.java.tmpl",
                    resourceName: resourceName, servicePackage: "${servicePackage}", resourcePathVar: resourceVarName
        }
        addResourcePathConstant(resourcePath, resourceVarName)

        basicProject.applyTemplate("src/componentTest/groovy/${servicePackagePath}/resources") {
            "${resourceName}ResourceSpec.groovy" template: "/templates/springboot/rest-resource-spec.groovy.tmpl",
                    resourceName: resourceName, servicePackage: "${servicePackage}"

            "${resourceName}ResourceWireSpec.groovy" template: "/templates/springboot/rest-resource-wirespec.groovy.tmpl",
                    resourceName: resourceName, servicePackage: "${servicePackage}"
        }
    }

    private void addResourcePathConstant(String resourcePath, String resourceVarName) {
        File resourcePathsFile = new File(basicProject.repoDir, "src/main/java/${servicePackagePath}/api/ResourcePaths.java")
        if (resourcePathsFile.exists() == false) {
            throw new GradleException("Failed to resolve ResurcePaths.java at expected location=${resourcePathsFile.absolutePath}")
        }
        String resourcePathsText = resourcePathsFile.text
        resourcePathsText = resourcePathsText.replaceAll(/(?m)\s*}\s*/, """
    public static final String ${resourceVarName} = "/${resourcePath}";

}
"""
        )
        resourcePathsFile.text = resourcePathsText
    }

}
