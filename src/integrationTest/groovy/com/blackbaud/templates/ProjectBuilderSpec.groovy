package com.blackbaud.templates

import com.blackbaud.templates.project.BasicProjectBuilder
import com.blackbaud.templates.project.RestProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.stream.Collectors

class ProjectBuilderSpec extends Specification {

    @Rule
    private TemporaryFolder rootFolder

    def setup() {
        GitRepo.init(rootFolder.root)
    }


    def "should be able to create a basic project"() {
        given:
        def expectedFileList = [".git", ".gitignore", "build.gradle", "gradle", "gradlew", "gradlew.bat", "src"]

        when:
        BasicProjectBuilder.instance
                .repoDir(rootFolder.root)
                .clean()
                .build()

        and:
        File[] files = rootFolder.root.listFiles()

        then:
        Arrays.asList(files).stream().map{f -> f.name}.collect(Collectors.toList()).containsAll(expectedFileList)
    }

    def "should be able to create a rest project"() {
        given:
        def expectedFileList = [".git", ".gitignore", "build.gradle", "gradle", "gradlew", "gradlew.bat", "src"]

        when:
        RestProjectBuilder.instance
                .repoDir(rootFolder.root)
                .useMybatis()
                .useKafka()
                .clean()
                .build()

        and:
        File[] files = rootFolder.root.listFiles()

        then:
        Arrays.asList(files).stream().map{f -> f.name}.collect(Collectors.toList()).containsAll(expectedFileList)
    }

}
