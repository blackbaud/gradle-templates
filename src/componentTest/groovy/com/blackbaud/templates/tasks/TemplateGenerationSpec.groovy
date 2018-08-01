package com.blackbaud.templates.tasks

import com.blackbaud.gradle.test.AbstractProjectSpecification
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import com.blackbaud.templates.GitRepo
import com.blackbaud.templates.ProjectProps

class TemplateGenerationSpec extends AbstractProjectSpecification {


    @Rule
    TemporaryFolder projectDir = new TemporaryFolder()
    File expectedTemplateContentDir = new File("src/componentTest/resources/expectedTemplateContent")
    boolean greenwash

    def setup() {
        project.ext["blackbaudGradleVersion"] = "2.14.1-bb.1.2"
        greenwash = Boolean.getBoolean("greenwash")
    }

    private BasicProject initBasicProject() {
        File serviceDir = new File(projectDir.root, "service")
        serviceDir.deleteDir()
        GitRepo repo = GitRepo.init(serviceDir)
        ProjectProps projectProps = new ProjectProps(project)
        BasicProject basicProject = new BasicProject(projectProps, repo)
        basicProject.initGradleProject()
        basicProject
    }

    private RestProject createRestProject() {
        BasicProject basicProject = initBasicProject()
        new RestProject(basicProject)
    }

    private RestProject initRestProject(boolean vsts = true) {
        RestProject restProject = createRestProject()
        restProject.initRestProject(false, vsts)
        restProject
    }

    private AsyncProject initAsyncProject() {
        RestProject restProject = initRestProject()
        new AsyncProject(restProject.basicProject)
    }

    private void greenwashOrAssertExpectedContent(RestProject restProject, String scenarioName) {
        greenwashOrAssertExpectedContent(restProject.basicProject, scenarioName)
    }

    private void greenwashOrAssertExpectedContent(AsyncProject asyncProject, String scenarioName) {
        greenwashOrAssertExpectedContent(asyncProject.basicProject, scenarioName)
    }

    private void greenwashOrAssertExpectedContent(BasicProject basicProject, String scenarioName) {
        greenwashOrAssertExpectedContent(basicProject.targetDir, scenarioName)
    }

    private void greenwashOrAssertExpectedContent(File actualTemplateDir, String scenarioName) {
        File expectedTemplateDir = new File(expectedTemplateContentDir, scenarioName)

        if (greenwash) {
            greenwashTestScenario(expectedTemplateDir, actualTemplateDir)
        } else {
            assertExpectedContent(expectedTemplateDir, actualTemplateDir)
        }
    }

    private void greenwashTestScenario(File expectedScenarioDir, File actualScenarioDir) {
        List<String> paths = getProjectFilePaths(actualScenarioDir)
        expectedScenarioDir.deleteDir()
        paths.each { String path ->
            File actualTemplateFile = new File(actualScenarioDir, path)
            File expectedTemplateFile = new File(expectedScenarioDir, path)
            if (actualTemplateFile.isDirectory()) {
                expectedTemplateFile.mkdirs()
            } else {
                expectedTemplateFile.parentFile.mkdirs()
                expectedTemplateFile.text = actualTemplateFile.text
            }
        }
    }

    private List<String> getProjectFilePaths(File projectDir) {
        List<String> paths = []
        projectDir.eachFileRecurse { File file ->
            String path = file.absolutePath - projectDir.absolutePath
            if (!path.startsWith("/.") && !path.startsWith("/gradle")) {
                paths << path
            }
        }
        paths
    }

    private void assertExpectedContent(File expectedScenarioDir, File actualScenarioDir) {
        List<String> actualPaths = getProjectFilePaths(actualScenarioDir)
        actualPaths.each { String path ->
            File actualFile = new File(actualScenarioDir, path)
            File expectedFile = new File(expectedScenarioDir, path)
            if (actualFile.isDirectory()) {
                assert expectedFile.isDirectory()
            } else {
                assertFileContentsEqual(expectedFile, actualFile)
            }
        }

        List<String> expectedPaths = getProjectFilePaths(expectedScenarioDir)
        assert actualPaths == expectedPaths
    }

    private void assertFileContentsEqual(File expected, File actual) {
        if (expected.text != actual.text) {
            List<String> expectedLines = expected.readLines()
            List<String> actualLines = actual.readLines()
            for (int i = 0; i < expectedLines.size() && i < actualLines.size(); i++) {
                assert expectedLines[i] == actualLines[i]
            }
            assert expected.text == actual.text
        }
    }

    def "should create library"() {
        given:
        BasicProject basicProject = initBasicProject()

        when:
        basicProject.initGradleProject()

        then:
        greenwashOrAssertExpectedContent(basicProject.targetDir, "library")
    }

    def "should create integration test"() {
        given:
        BasicProject basicProject = initBasicProject()
        IntegrationTestProject integrationTestProject = new IntegrationTestProject(basicProject)

        when:
        integrationTestProject.initIntegrationTestProject(true, true)

        then:
        greenwashOrAssertExpectedContent(basicProject.targetDir, "integration-test")
    }

    def "should create deployable vsts project"() {
        given:
        RestProject restProject = createRestProject()

        when:
        restProject.initRestProject(false, false)

        then:
        greenwashOrAssertExpectedContent(restProject, "deployable")
    }

    def "should create deployable project with postgres"() {
        given:
        RestProject restProject = createRestProject()

        when:
        restProject.initRestProject(false, false)
        restProject.initPostgres()
        restProject.initMybatis()

        then:
        greenwashOrAssertExpectedContent(restProject, "deployable-with-postgres")
    }

    def "should create deployable project with kafka"() {
        given:
        RestProject restProject = createRestProject()

        when:
        restProject.initRestProject(false, false)
        restProject.initKafka()

        then:
        greenwashOrAssertExpectedContent(restProject, "deployable-with-kafka")
    }

    def "should create deployable project with cosmos"() {
        given:
        RestProject restProject = createRestProject()

        when:
        restProject.initRestProject(false, false)
        restProject.initCosmos()

        then:
        greenwashOrAssertExpectedContent(restProject, "deployable-with-cosmos")
    }

    def "should add service bus internal datasync topic"() {
        given:
        AsyncProject asyncProject = initAsyncProject()

        when:
        asyncProject.addInternalTopic("Datasync", AsyncProject.TopicType.DATASYNC, false)

        then:
        greenwashOrAssertExpectedContent(asyncProject, "service-bus-internal-topic-datasync")
    }

    def "should add service bus internal schedule topic"() {
        given:
        AsyncProject asyncProject = initAsyncProject()

        when:
        asyncProject.addInternalTopic("Scheduled", AsyncProject.TopicType.SCHEDULE, false)

        then:
        greenwashOrAssertExpectedContent(asyncProject, "service-bus-internal-topic-scheduled")
    }

    def "should add service bus consumer"() {
        given:
        AsyncProject asyncProject = initAsyncProject()

        when:
        asyncProject.addExternalTopic("Consumer", AsyncProject.TopicType.SCHEDULE, true, false, false)

        then:
        greenwashOrAssertExpectedContent(asyncProject, "service-bus-consumer")
    }

    def "should add service bus producer"() {
        given:
        AsyncProject asyncProject = initAsyncProject()

        when:
        asyncProject.addExternalTopic("Producer", AsyncProject.TopicType.DATASYNC, false, true, false)

        then:
        greenwashOrAssertExpectedContent(asyncProject, "service-bus-producer")
    }

    def "should add service bus with sessions enabled"() {
        given:
        AsyncProject asyncProject = initAsyncProject()

        when:
        asyncProject.addExternalTopic("Producer", AsyncProject.TopicType.DATASYNC, true, false, true)

        then:
        greenwashOrAssertExpectedContent(asyncProject, "service-bus-sessions-enabled")
    }

    def "should add jpa object"() {
        given:
        RestProject restProject = initRestProject()

        when:
        restProject.addJpaEntityObject("Account")

        then:
        greenwashOrAssertExpectedContent(restProject, "add-resource-jpa-entity")
    }

    def "should add rest api object"() {
        given:
        RestProject restProject = initRestProject()

        when:
        restProject.addApiObject("User")

        then:
        greenwashOrAssertExpectedContent(restProject, "add-resource-rest-api")
    }

    def "should add kafka message"() {
        given:
        RestProject restProject = initRestProject()
        KafkaProject kafkaProject = new KafkaProject(restProject.basicProject)

        when:
        kafkaProject.addApiObject("User")

        then:
        greenwashOrAssertExpectedContent(kafkaProject.basicProject, "add-resource-kafka-message")
    }

}
