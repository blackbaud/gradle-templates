package templates.tasks

import org.gradle.api.tasks.TaskAction


class AddPostgresContainerTask extends AbstractTemplateTask {

    AddPostgresContainerTask() {
        super("Add a Postgres container and default configuration to an existing project")
    }

    @TaskAction
    void addPostgresContainer() {
        BasicProject basicProject = openBasicProject()

        File buildFile = basicProject.getProjectFileOrFail("build.gradle")
        buildFile.append("""
buildscript {
    dependencies {
        classpath "com.blackbaud:gradle-docker:1.+"
    }
}

apply plugin: "docker"

docker {
    container {
        imageName "postgres:9.4"
        publish "5432:5432"
        env "POSTGRES_USER=postgres"
        env "POSTGRES_PASSWORD=postgres"
    }
}

componentTest.dependsOn startPostgres
""")

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application.properties")
        if (applicationPropertiesFile.exists()) {
            applicationPropertiesFile.append("""
spring.datasource.url=jdbc:postgresql://local.docker:5432/
spring.datasource.username=postgres
spring.datasource.password=postgres
""")
        }
    }

}
