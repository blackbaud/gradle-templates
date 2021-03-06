package com.blackbaud.templates.project

import com.blackbaud.templates.CurrentVersions

import static com.google.common.base.CaseFormat.LOWER_CAMEL
import static com.google.common.base.CaseFormat.LOWER_HYPHEN
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE
import static com.google.common.base.CaseFormat.UPPER_CAMEL

class RestProject {

    private BasicProject basicProject

    RestProject(BasicProject basicProject) {
        this.basicProject = basicProject
    }

    ProjectProps getProjectProps() {
        basicProject.projectProps
    }

    String getServiceId() {
        "${UPPER_CAMEL.to(LOWER_HYPHEN, serviceName)}"
    }

    String getServiceName() {
        basicProject.serviceName
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

    void initRestProject(boolean shouldDisableAuthFilter) {
        basicProject.initGradleProject()

        basicProject.addLombokConfig()
        basicProject.commitProjectFiles("add lombok config")

        createRestBase()

        if (shouldDisableAuthFilter) {
            disableAuthFilter()
        } else {
            enableAuthFilter()
        }
    }

    private void enableAuthFilter() {
        File applicationProperties = basicProject.getProjectFileOrFail("src/main/resources/application.properties")
        applicationProperties << """\
bbauth.enabled=true
"""

        basicProject.commitProjectFiles("enable auth filter")
    }

    private void disableAuthFilter() {
        File applicationProperties = basicProject.getProjectFileOrFail("src/main/resources/application.properties")
        applicationProperties << """
authorization.filter.enable=false
"""
        basicProject.commitProjectFiles("disable auth filter")
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

    void initCosmos() {
        DatasourceProject datasourceProject = new DatasourceProject(this)
        datasourceProject.initCosmos()

        basicProject.commitProjectFiles("initialize cosmos container")
    }

    void initKafka() {
        KafkaProject kafkaProject = new KafkaProject(basicProject)
        kafkaProject.initKafka()

        basicProject.commitProjectFiles("initialize kafka")
    }

    private void createRestBase() {
        basicProject.applyTemplate("src/main/java/${servicePackagePath}") {
            "${serviceName}.java" template: "/templates/springboot/rest/application-class.java.tmpl",
                    serviceName: serviceName, servicePackage: servicePackage
        }
        basicProject.applyTemplate("src/main/java/${servicePackagePath}/core") {
            "CoreConfig.java" template: "/templates/springboot/rest/application-core-config.java.tmpl",
                    packageName: "${servicePackage}.core"
        }

        basicProject.applyTemplate("src/componentTest/groovy/${servicePackagePath}") {
            "ComponentTest.java" template: "/templates/springboot/rest/component-test-annotation.java.tmpl",
                    serviceName: serviceName, packageName: servicePackage

            "ComponentTestConfig.java" template: "/templates/springboot/rest/application-test-config.java.tmpl",
                    className: "ComponentTestConfig", serviceName: serviceName, packageName: servicePackage
        }
        basicProject.applyTemplate("src/componentTest/groovy/com/blackbaud/swagger") {
            "GenerateSwaggerDocsSpec.groovy" template: "/templates/springboot/rest/generate-swagger-docs-spec.groovy.tmpl",
                     servicePackage: servicePackage
        }

        basicProject.applyTemplate("src/sharedTest/groovy/${servicePackagePath}/core") {
            "CoreARandom.java" template: "/templates/test/core-arandom.java.tmpl",
                    servicePackageName: servicePackage
            "CoreRandomBuilderSupport.java" template: "/templates/test/random-builder-support.java.tmpl",
                    packageName: "${servicePackage}.core", qualifier: "Core"
        }

        basicProject.applyTemplate {
            'build.gradle'([template          : "/templates/springboot/rest/build.gradle.tmpl",
                            servicePackageName: servicePackage
                           ] + CurrentVersions.VERSION_MAP)

            'gradle.properties' template: "/templates/basic/gradle.properties.tmpl",
                                artifactId: serviceId
            'src' {
                'main' {
                    'resources' {
                        'application.properties' template: "/templates/springboot/rest/application.properties.tmpl",
                                                 serviceId: "${serviceId}", resourcePackageName: "${servicePackage}.resources"
                        'logback-spring.xml' template: "/templates/logback/logback-vsts.tmpl"
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

    void createResource(String resourceName, boolean addEntity, boolean addWireSpec) {
        addResourceAndSupportingClasses(resourceName, addWireSpec)

        ProjectFile resourceFile = basicProject.findFile("${resourceName}Resource.java")
        resourceFile.appendAfterLine("class", """\

    @GetMapping("/{id}")
    public ${resourceName} find(@PathVariable("id") UUID id) {
        throw new IllegalStateException("implement");
    }
""")

        ProjectFile clientFile = basicProject.findFile("${resourceName}Client.java")
        clientFile.appendAfterLine("class", """\

    @RequestLine("GET /{id}")
    ${resourceName} find(@Param("id") UUID id);
""")

        addApiObject(resourceName)

        if (addEntity) {
            addJpaEntityObject(resourceName)
        }
    }

    private void addResourceAndSupportingClasses(String resourceName, boolean addWireSpec) {
        String resourcePath = "${UPPER_CAMEL.to(LOWER_UNDERSCORE, resourceName)}"
        String resourceVarName = "${resourcePath.toUpperCase()}_PATH"
        String resourceNameLowerCamel = UPPER_CAMEL.to(LOWER_CAMEL, resourceName)

        basicProject.addClientSubmodule("rest")

        addResourcePathConstant(resourcePath, resourceVarName)

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/resources") {
            "${resourceName}Resource.java" template: "/templates/springboot/rest/resource.java.tmpl",
                                           resourceName: resourceName, servicePackage: "${servicePackage}", resourcePathVar: resourceVarName
        }
        basicProject.applyTemplate("rest-client/src/main/java/${servicePackagePath}/client") {
            "${resourceName}Client.java" template: "/templates/springboot/rest/resource-client.java.tmpl",
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
        ProjectFile testConfig = basicProject.findOptionalFile("ComponentTestConfig.java")
        if (testConfig == null) {
            testConfig = basicProject.findFile("TestConfig.java")
        }
        testConfig.addImport("org.springframework.context.annotation.Bean")
        testConfig.addImport("${servicePackage}.client.${resourceName}Client")
        testConfig.appendToClass("""
    @Bean
    public ${resourceName}Client ${resourceNameLowerCamel}Client() {
        return testClientSupport.createClientWithBbAuthToken(${resourceName}Client.class);
    }
""")
    }

    private void addResourcePathConstant(String resourcePath, String resourceVarName) {
        ProjectFile resourcePathsFile = basicProject.getProjectFile("rest-client/src/main/java/${servicePackagePath}/api/ResourcePaths.java")
        if (resourcePathsFile.exists() == false) {
            basicProject.applyTemplate("rest-client/src/main/java/${servicePackagePath}/api") {
                'ResourcePaths.java' template: "/templates/springboot/rest/resource-paths.java.tmpl",
                        packageName: "${servicePackage}.api"
            }
        }

        resourcePathsFile.appendToClass("""
    public static final String ${resourceVarName} = "/${resourcePath}";
""")
    }

    void addJpaEntityObject(String resourceName) {
        String resourcePath = "${UPPER_CAMEL.to(LOWER_UNDERSCORE, resourceName)}"

        BuildFile buildFile = basicProject.buildFile
        buildFile.addDependencyAfter("compile",
                                     '"com.blackbaud:common-deployable-spring-boot-jpa:${commonSpringBootVersion}"',
                                     ":common-deployable-spring-boot-rest:")

        ProjectFile coreConfigClass = basicProject.findFile("CoreConfig.java")
        coreConfigClass.addConfigurationImport("com.blackbaud.boot.jpa.JpaConfiguration")
        coreConfigClass.addEntityScanAndEnableJpaRepositories("${servicePackage}.core.domain")

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/core/domain") {
            "${resourceName}Entity.java" template: "/templates/springboot/rest/jpa/jpa-entity.java.tmpl",
                                         resourceName: resourceName, packageName: "${servicePackage}.core.domain", tableName: resourcePath
        }

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/core/domain") {
            "${resourceName}Repository.java" template: "/templates/springboot/rest/jpa/jpa-repository.java.tmpl",
                                             resourceName: resourceName, packageName: "${servicePackage}.core.domain"
        }

        addRandomBuilder(resourceName)

        DatasourceProject datasourceProject = new DatasourceProject(this)
        datasourceProject.addCreateTableScript(resourcePath)
    }

    private void addRandomBuilder(String entityName) {
        String entityNameLowerCamel = UPPER_CAMEL.to(LOWER_CAMEL, entityName)

        basicProject.applyTemplate("src/sharedTest/groovy/${servicePackagePath}/core/domain") {
            "Random${entityName}EntityBuilder.groovy" template: "/templates/test/random-core-builder.groovy.tmpl",
                                                      resourceName: entityName,
                                                      resourceNameLowerCamel: entityNameLowerCamel,
                                                      servicePackageName: servicePackage
        }

        ProjectFile randomCoreBuilderSupport = basicProject.findFile("CoreRandomBuilderSupport.java")
        randomCoreBuilderSupport.addImport("${servicePackage}.core.domain.${entityName}Repository")
        randomCoreBuilderSupport.addImport("${servicePackage}.core.domain.Random${entityName}EntityBuilder")
        randomCoreBuilderSupport.addImport("org.springframework.beans.factory.annotation.Autowired")
        randomCoreBuilderSupport.appendToClass("""
    @Autowired
    private ${entityName}Repository ${entityNameLowerCamel}Repository;

    public Random${entityName}EntityBuilder ${entityNameLowerCamel}Entity() {
        return new Random${entityName}EntityBuilder(${entityNameLowerCamel}Repository);
    }
""")
    }

    void addApiObject(String resourceName, boolean upperCamel = false) {
        basicProject.addExternalApiObject("rest", resourceName, upperCamel)
    }

    void addCosmosEntityObject(String entityName, boolean auditable) {
        String entityNameLowerCamel = UPPER_CAMEL.to(LOWER_CAMEL, entityName)

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/core/domain") {
            String entityNameLowerUnderscore = UPPER_CAMEL.to(LOWER_UNDERSCORE, entityName)
            "${entityName}Entity.java" template: "/templates/springboot/rest/mongo/mongo-entity.java.tmpl",
                                         resourceName: entityName, packageName: "${servicePackage}.core.domain",
                                         collectionName: "${entityNameLowerUnderscore}s", auditable: auditable
        }

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/core/domain") {
            "${entityName}Repository.java" template: "/templates/springboot/rest/mongo/mongo-repository.java.tmpl",
                                             entityName: entityName, packageName: "${servicePackage}.core.domain"
        }
        basicProject.applyTemplate("src/main/java/${servicePackagePath}/core/domain") {
            "${entityName}TransactionalRepository.java" template: "/templates/springboot/rest/mongo/mongo-transactional-repository.java.tmpl",
                                             entityName: entityName, packageName: "${servicePackage}.core.domain"
        }

        if (basicProject.findOptionalFile("CosmosConfig.java") == null) {
            new DatasourceProject(this).initCosmos()
        }

        ProjectFile cosmosConfig = basicProject.findFile("CosmosConfig.java")
        cosmosConfig.appendToClass("""
    @Bean
    public ${entityName}Repository ${entityNameLowerCamel}Repository(CosmosRetryableRepositoryFactory factory, ${entityName}TransactionalRepository transactionalRepository) {
        return factory.createRepository(transactionalRepository, ${entityName}Repository.class);
    }
""")

        addRandomBuilder(entityName)
    }

    void addPermissions() {
        checkForRestClient()
        addPermissionsToRestClient()
        addPermissionsConfig()
        addPermissionsConfigToMainClass()
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
        basicProject.applyTemplate("src/main/java/${servicePackagePath}") {
            "PermissionsConfig.java" template: "/templates/springboot/rest/permissions/permissions-config.java.tmpl",
                                     packageName: servicePackage,
                                     serviceName: serviceName
        }
    }

    private void addPermissionsConfigToMainClass() {
        ProjectFile coreConfigClass = basicProject.findFile("${serviceName}.java")
        coreConfigClass.addConfigurationImport("${servicePackage}.PermissionsConfig", true)
    }
}
