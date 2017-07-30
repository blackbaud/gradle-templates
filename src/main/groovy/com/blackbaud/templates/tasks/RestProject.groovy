package com.blackbaud.templates.tasks

import static com.google.common.base.CaseFormat.LOWER_CAMEL
import static com.google.common.base.CaseFormat.LOWER_HYPHEN
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE
import static com.google.common.base.CaseFormat.UPPER_CAMEL

class RestProject {

    private BasicProject basicProject
    private String serviceName
    private String serviceId

    RestProject(BasicProject basicProject) {
        this(basicProject, basicProject.serviceName)
    }

    RestProject(BasicProject basicProject, String serviceName) {
        this.basicProject = basicProject
        this.serviceName = serviceName.capitalize()
        this.serviceId = "${UPPER_CAMEL.to(LOWER_HYPHEN, serviceName)}"
    }

    String getServiceId() {
        serviceId
    }

    String getServiceName() {
        serviceName
    }

    String getServicePackage() {
        basicProject.servicePackage
    }

    String getServicePackagePath() {
        basicProject.servicePackagePath
    }

    BasicProject getBasicProject() {
        basicProject
    }

    void initRestProject() {
        basicProject.initGradleProject()
        createRestBase()
    }

    void initPostgres() {
        DatasourceProject datasourceProject = new DatasourceProject(this)
        datasourceProject.initPostgres()

        basicProject.commitProjectFiles("initialize postgres container")
    }

    void initMybatis() {
        DatasourceProject datasourceProject = new DatasourceProject(this)
        datasourceProject.initMybatis()

        basicProject.commitProjectFiles("initialize mybatis")
    }

    void initKafka() {
        KafkaProject kafkaProject = new KafkaProject(basicProject)
        kafkaProject.initKafka()

        basicProject.commitProjectFiles("initialize kafka")
    }

    private void createRestBase() {
        addResourcePaths()

        basicProject.applyTemplate("src/main/java/${servicePackagePath}") {
            "${serviceName}.java" template: "/templates/springboot/application-class.java.tmpl",
                    serviceName: serviceName, servicePackage: servicePackage
        }

        basicProject.applyTemplate("src/main/resources") {
            "bootstrap.properties" template: "/templates/springboot/bootstrap.properties.tmpl", serviceId: "${serviceId}"
        }

        basicProject.applyTemplate("src/main/resources") {
            "bootstrap-cloud.properties" template: "/templates/springboot/bootstrap-cloud.properties.tmpl"
        }

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/config") {
            "JerseyConfig.java" template: "/templates/springboot/rest/jersey-config.java.tmpl",
                                servicePackage: "${servicePackage}"
        }

        basicProject.applyTemplate("src/deploy/cloudfoundry") {
            "app-descriptor.yml" template: "/templates/deploy/app-descriptor.yml.tmpl"
        }

        basicProject.applyTemplate("src/componentTest/java/${servicePackagePath}") {
            "ComponentTest.java" template: "/templates/springboot/rest/component-test-annotation.java.tmpl",
                    serviceName: serviceName, packageName: servicePackage

            "TestConfig.java" template: "/templates/springboot/rest/application-test-config.java.tmpl",
                    className: "TestConfig", packageName: servicePackage
        }

        basicProject.applyTemplate("src/mainTest/groovy/${servicePackagePath}/api") {
            "ClientARandom.java" template: "/templates/test/client-arandom.java.tmpl",
                    packageName: "${servicePackage}.api"
            "RandomClientBuilderSupport.java" template: "/templates/test/random-builder-support.java.tmpl",
                    packageName: "${servicePackage}.api", qualifier: "Client"
        }
        basicProject.applyTemplate("src/mainTest/groovy/${servicePackagePath}/core") {
            "CoreARandom.java" template: "/templates/test/core-arandom.java.tmpl",
                    servicePackageName: servicePackage
            "RandomCoreBuilderSupport.java" template: "/templates/test/random-builder-support.java.tmpl",
                    packageName: "${servicePackage}.core", qualifier: "Core"
        }

        basicProject.applyTemplate {
            'build.gradle' template: "/templates/springboot/rest/build.gradle.tmpl",
                    servicePackageName: servicePackage
            'gradle.properties' template: "/templates/basic/gradle.properties.tmpl",
                    artifactId: serviceId
            'src' {
                'main' {
                    'resources' {
                        'application.properties' template: "/templates/springboot/rest/application.properties.tmpl"
                        'logback.xml' template: "/templates/logback/logback.tmpl"
                    }
                }
                'test' {
                    "groovy" {}
                }
                'componentTest' {
                    'groovy' {}
                }
            }
        }

        basicProject.commitProjectFiles("springboot rest bootstrap")
    }

    private void addResourcePaths() {
        basicProject.applyTemplate("src/main/java/${servicePackagePath}/api") {
            'ResourcePaths.java' template: "/templates/springboot/rest/resource-paths.java.tmpl",
                    packageName: "${servicePackage}.api"
        }
    }

    void createEmbeddedService(boolean addEntity) {
        addResourcePaths()
        createCrudResource(serviceName, addEntity)

        println "********************************************************"
        println "********************************************************"
        println ""
        println "Remember to add ${servicePackage} to @ComponentScan list!"
        println ""
        println "********************************************************"
        println "********************************************************"
    }

    void createCrudResource(String resourceName, boolean addEntity, boolean addWireSpec) {
        String resourcePath = "${UPPER_CAMEL.to(LOWER_UNDERSCORE, resourceName)}"
        String resourceVarName = "${resourcePath.toUpperCase()}_PATH"
        String resourceNameLowerCamel = UPPER_CAMEL.to(LOWER_CAMEL, resourceName)

        addResourcePathConstant(resourcePath, resourceVarName)
        basicProject.applyTemplate("src/main/java/${servicePackagePath}/resources") {
            "${resourceName}Resource.java" template: "/templates/springboot/rest/resource-crud.java.tmpl",
                    resourceName: resourceName, servicePackage: "${servicePackage}", resourcePathVar: resourceVarName
        }
        basicProject.applyTemplate("src/main/java/${servicePackagePath}/client") {
            "${resourceName}Client.java" template: "/templates/springboot/rest/resource-crud-client.java.tmpl",
                                         resourceName: resourceName, servicePackage: "${servicePackage}", resourcePathVar: resourceVarName
        }
        basicProject.applyTemplate("src/componentTest/groovy/${servicePackagePath}/resources") {
            "${resourceName}ResourceSpec.groovy" template: "/templates/springboot/rest/resource-spec.groovy.tmpl",
                    resourceName: resourceName, servicePackage: "${servicePackage}"
        }
        if (addWireSpec) {
            basicProject.applyTemplate("src/componentTest/groovy/${servicePackagePath}/resources") {
                "${resourceName}ResourceWireSpec.groovy" template: "/templates/springboot/rest/resource-wirespec.groovy.tmpl",
                        resourceName: resourceName, servicePackage: "${servicePackage}"
            }
        }
        File testConfig = basicProject.findFile("TestConfig.java")
        FileUtils.appendAfterLine(testConfig, /import.*/,
                                  """import org.springframework.context.annotation.Bean;
import ${servicePackage}.client.${resourceName}Client;
"""

        )
        FileUtils.appendToClass(testConfig, """

    @Bean
    public ${resourceName}Client ${resourceNameLowerCamel}Client() {
        return testClientSupport().createClientWithTestToken(${resourceName}Client.builder());
    }
""")

        addApiObject(resourceName)

        FileUtils.appendAfterLine(basicProject.findFile("JerseyConfig.java"), /packages\s*\(/, "        packages(\"${servicePackage}.resources\");")

        if (addEntity) {
            basicProject.applyTemplate("src/main/java/${servicePackagePath}/core/domain") {
                "${resourceName}Entity.java" template: "/templates/springboot/rest/resource-entity.java.tmpl",
                        resourceName: resourceName, packageName: "${servicePackage}.core.domain", tableName: resourcePath
            }

            basicProject.applyTemplate("src/mainTest/groovy/${servicePackagePath}/core/domain") {
                "Random${resourceName}EntityBuilder.groovy" template: "/templates/test/random-core-builder.groovy.tmpl",
                        targetClass: "${resourceName}Entity", servicePackageName: servicePackage
            }

            File randomCoreBuilderSupport = basicProject.findFile("RandomCoreBuilderSupport.java")
            FileUtils.appendAfterLine(randomCoreBuilderSupport, "package", "import ${servicePackage}.core.domain.Random${resourceName}EntityBuilder;")
            FileUtils.appendToClass(randomCoreBuilderSupport, """

    public Random${resourceName}EntityBuilder ${resourceNameLowerCamel}Entity() {
        return new Random${resourceName}EntityBuilder();
    }
""")

            DatasourceProject datasourceProject = new DatasourceProject(this)
            datasourceProject.addCreateTableScript(resourcePath)
        }
    }

    void addApiObject(String resourceName, boolean upperCamel = false) {
        String resourceNameLowerCamel = UPPER_CAMEL.to(LOWER_CAMEL, resourceName)

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/api") {
            "${resourceName}.java" template: "/templates/springboot/rest/resource-api.java.tmpl",
                                   resourceName: resourceName, packageName: "${servicePackage}.api",
                                   upperCamel: upperCamel
        }
        basicProject.applyTemplate("src/mainTest/groovy/${servicePackagePath}/api") {
            "Random${resourceName}Builder.groovy" template: "/templates/test/random-client-builder.groovy.tmpl",
                                                  targetClass: resourceName, servicePackageName: servicePackage
        }
        File randomClientBuilderSupport = basicProject.findFile("RandomClientBuilderSupport.java")
        FileUtils.appendAfterLine(randomClientBuilderSupport, "package", "import ${servicePackage}.api.Random${resourceName}Builder;")
        FileUtils.appendToClass(randomClientBuilderSupport, """

    public Random${resourceName}Builder ${resourceNameLowerCamel}() {
        return new Random${resourceName}Builder();
    }
""")
    }

    void createBasicResource(String resourceName, boolean addWireSpec) {
        String resourcePath = "${UPPER_CAMEL.to(LOWER_UNDERSCORE, resourceName)}"
        String resourceVarName = "${resourcePath.toUpperCase()}_PATH"
        String resourceNameLowerCamel = UPPER_CAMEL.to(LOWER_CAMEL, resourceName)

        addResourcePathConstant(resourcePath, resourceVarName)
        basicProject.applyTemplate("src/main/java/${servicePackagePath}/resources") {
            "${resourceName}Resource.java" template: "/templates/springboot/rest/resource-basic.java.tmpl",
                    resourceName: resourceName, servicePackage: "${servicePackage}", resourcePathVar: resourceVarName
        }
        basicProject.applyTemplate("src/main/java/${servicePackagePath}/client") {
            "${resourceName}Client.java" template: "/templates/springboot/rest/resource-basic-client.java.tmpl",
                                         resourceName: resourceName, servicePackage: "${servicePackage}", resourcePathVar: resourceVarName
        }
        basicProject.applyTemplate("src/componentTest/groovy/${servicePackagePath}/resources") {
            "${resourceName}ResourceSpec.groovy" template: "/templates/springboot/rest/resource-spec.groovy.tmpl",
                    resourceName: resourceName, servicePackage: "${servicePackage}"
        }
        if (addWireSpec) {
            basicProject.applyTemplate("src/componentTest/groovy/${servicePackagePath}/resources") {
                "${resourceName}ResourceWireSpec.groovy" template: "/templates/springboot/rest/resource-wirespec.groovy.tmpl",
                        resourceName: resourceName, servicePackage: "${servicePackage}"
            }
        }
        File testConfig = basicProject.findFile("TestConfig.java")
        FileUtils.appendAfterLine(testConfig, /import.*/,
                                  """import org.springframework.context.annotation.Bean;
import ${servicePackage}.client.${resourceName}Client;
"""

        )
        FileUtils.appendToClass(testConfig, """

    @Bean
    public ${resourceName}Client ${resourceNameLowerCamel}Client() {
        return testClientSupport().createClientWithTestToken(${resourceName}Client.builder());
    }
""")

        FileUtils.appendAfterLine(basicProject.findFile("JerseyConfig.java"), /packages\s*\(/, "        packages(\"${servicePackage}.resources\");")
    }

    private void addResourcePathConstant(String resourcePath, String resourceVarName) {
        File resourcePathsFile = basicProject.getProjectFile("src/main/java/${servicePackagePath}/api/ResourcePaths.java")
        if (resourcePathsFile.exists() == false) {
            addResourcePaths()
        }

        FileUtils.appendToClass(resourcePathsFile, """
    public static final String ${resourceVarName} = "/${resourcePath}";
""")
    }

}
