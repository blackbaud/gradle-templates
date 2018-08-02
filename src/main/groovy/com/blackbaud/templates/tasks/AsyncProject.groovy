package com.blackbaud.templates.tasks

import com.blackbaud.templates.CurrentVersions
import com.google.common.base.CaseFormat


class AsyncProject {

    private BasicProject basicProject

    AsyncProject(BasicProject basicProject) {
        this.basicProject = basicProject
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

    private void initServiceBusIfNotAlreadyInitialized() {
        if (basicProject.getBuildFile().text =~ /common-async-service-bus/) {
            return
        }

        FileUtils.appendAfterLastLine(basicProject.getBuildFile(), /ext \{/,
                "        commonAsyncServiceBusVersion = \"${CurrentVersions.COMMON_ASYNC_MAJOR_VERSION}.+\"")
        FileUtils.appendAfterLine(basicProject.getBuildFile(), /compile.*common-spring-boot/,
                '    compile "com.blackbaud:common-async-service-bus:${commonAsyncServiceBusVersion}"')
        FileUtils.appendAfterLine(basicProject.getBuildFile(), /sharedTestCompile/,
                '    sharedTestCompile "com.blackbaud:common-async-service-bus-test:${commonAsyncServiceBusVersion}"')

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application-local.properties")
        if ((applicationPropertiesFile.exists() && applicationPropertiesFile.text.contains("servicebus.stub")) == false) {
            applicationPropertiesFile.append("""
servicebus.stub=true
""")
        }

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/servicebus") {
            "ServiceBusConfig.java" template: "/templates/springboot/service-bus/service-bus-config.java.tmpl",
                                    packageName: "${servicePackage}.servicebus"
        }
    }

    void addInternalTopic(String topicName, TopicType topicType, boolean sessionEnabled) {
        addTopic(topicName, topicType, true, true, true, sessionEnabled)
    }

    void addExternalTopic(String topicName, TopicType topicType, boolean consumer, boolean publisher, boolean sessionEnabled) {
        addTopic(topicName, topicType, false, consumer, publisher, sessionEnabled)
    }

    private void addTopic(String topicName, TopicType topicType, boolean internal, boolean consumer, boolean publisher, boolean sessionEnabled) {
        initServiceBusIfNotAlreadyInitialized()

        ServiceBusNameResolver formatter = new ServiceBusNameResolver(topicName)
        File componentTestConfigFile = basicProject.findComponentTestConfig()

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application.properties")
        applicationPropertiesFile.append("""\
servicebus.${formatter.topicNameSnakeCase}.session_enabled=${sessionEnabled}
""")

        File applicationLocalPropertiesFile = basicProject.getProjectFile("src/main/resources/application-local.properties")
        applicationLocalPropertiesFile.append("""\
servicebus.${formatter.topicNameSnakeCase}.producer_connection_url=Endpoint=sb://test.servicebus.windows.net/;SharedAccessSignature=SharedAccessSignature sr=amqp%3A%2F%2Ftest.servicebus.windows.net%2Ftest&sig=test
""")
        applicationLocalPropertiesFile.append("""\
servicebus.${formatter.topicNameSnakeCase}.consumer_connection_url=Endpoint=sb://test.servicebus.windows.net/;SharedAccessSignature=SharedAccessSignature sr=amqp%3A%2F%2Ftest.servicebus.windows.net%2Ftest%2Fsubscriptions%2Ftest&sig=test
""")
        if (publisher) {
            applicationPropertiesFile.append("""\
servicebus.${formatter.topicNameSnakeCase}.producer_connection_url=\${APPSETTING_ServiceBus__${formatter.topicNameSnakeCase}__Send}
""")
        }
        if (consumer) {
            applicationPropertiesFile.append("""\
servicebus.${formatter.topicNameSnakeCase}.consumer_connection_url=\${APPSETTING_ServiceBus__${formatter.topicNameSnakeCase}__Listen}
""")
        }

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/servicebus") {
            "${formatter.propertiesClassName}.java" template: "/templates/springboot/service-bus/service-bus-properties.java.tmpl",
                                                    packageName: "${servicePackage}.servicebus",
                                                    className: formatter.propertiesClassName,
                                                    topicPrefix: "servicebus.${formatter.topicNameSnakeCase}"
        }

        File serviceBusConfigFile = basicProject.findFile("ServiceBusConfig.java")
        FileUtils.appendBeforeLine(serviceBusConfigFile, "public class", "@EnableConfigurationProperties(${formatter.propertiesClassName}.class)")

        File applicationClass = basicProject.findFile("${basicProject.serviceName}.java")
        FileUtils.addImport(applicationClass, "${servicePackage}.servicebus.ServiceBusConfig")
        FileUtils.addImport(applicationClass, "org.springframework.context.annotation.Import")
        FileUtils.addConfigurationImport(applicationClass, "ServiceBusConfig.class")

        File publisherConfigFile
        if (publisher) {
            publisherConfigFile = serviceBusConfigFile
            if (internal) {
                basicProject.addInternalApiObject("service-bus", formatter.payloadClassName, false)
            } else {
                basicProject.addExternalApiObject("service-bus", formatter.payloadClassName, false)
            }
        } else {
            publisherConfigFile = componentTestConfigFile
            FileUtils.addImport(publisherConfigFile, "${servicePackage}.servicebus.${formatter.propertiesClassName}")
        }

        FileUtils.addImport(publisherConfigFile, "org.springframework.beans.factory.annotation.Qualifier")
        FileUtils.addImport(publisherConfigFile, "com.blackbaud.azure.servicebus.publisher.JsonMessagePublisher")
        FileUtils.addImport(publisherConfigFile, "com.blackbaud.azure.servicebus.config.ServiceBusProperties")
        FileUtils.addImport(publisherConfigFile, "com.blackbaud.azure.servicebus.publisher.ServiceBusPublisherBuilder")
        FileUtils.appendToClass(publisherConfigFile, """
    @Bean
    public JsonMessagePublisher ${formatter.topicNameCamelCase}Publisher(
            ServiceBusPublisherBuilder.Factory serviceBusPublisherFactory,
            ${formatter.propertiesClassName} serviceBusProperties) {
        return serviceBusPublisherFactory.create()${if (sessionEnabled) {"""
                .supportsSessions(${formatter.payloadClassName}::getId)"""} else { "" } }
                .buildJsonPublisher(serviceBusProperties);
    }
""")

        if (consumer) {
            basicProject.applyTemplate("src/main/java/${servicePackagePath}/servicebus") {
                "${formatter.messageHandlerClassName}.java" template: "/templates/springboot/service-bus/message-handler.java.tmpl",
                                                            packageName: "${servicePackage}.servicebus",
                                                            className: formatter.messageHandlerClassName,
                                                            payloadClassName: formatter.payloadClassName
            }

            addConsumerImports(serviceBusConfigFile)
            FileUtils.appendToClass(serviceBusConfigFile, """
    @Bean
    public ${formatter.messageHandlerClassName} ${formatter.topicNameCamelCase}MessageHandler() {
        return new ${formatter.messageHandlerClassName}();
    }

    @Bean
    public ServiceBusConsumer ${formatter.topicNameCamelCase}Consumer(
            ServiceBusConsumerBuilder.Factory serviceBusConsumerFactory,
            ${formatter.messageHandlerClassName} ${formatter.topicNameCamelCase}MessageHandler,
            ${formatter.propertiesClassName} serviceBusProperties) {
        return serviceBusConsumerFactory.create()
                .${topicType.consumerBuilderMethodName}(serviceBusProperties)
                .jsonMessageHandler(${formatter.topicNameCamelCase}MessageHandler, ${formatter.payloadClassName}.class)
                .build();
    }
""")
        } else {
            addConsumerImports(componentTestConfigFile)
            FileUtils.addImport(componentTestConfigFile, "com.blackbaud.azure.servicebus.consumer.handlers.ValidatingServiceBusMessageHandler")
            FileUtils.appendToClass(componentTestConfigFile, """
    @Bean
    public ValidatingServiceBusMessageHandler<${formatter.payloadClassName}> ${formatter.messageHandlerClassName}() {
        return new ValidatingServiceBusMessageHandler<>("${formatter.topicNameCamelCase}Handler");
    }

    @Bean
    public ServiceBusConsumer sessionConsumer(
            ServiceBusConsumerBuilder.Factory serviceBusConsumerFactory,
            ${formatter.propertiesClassName} serviceBusProperties,
            @Qualifier("${formatter.topicNameCamelCase}MessageHandler") ValidatingServiceBusMessageHandler<${formatter.payloadClassName}> messageHandler) {
        return serviceBusConsumerFactory.create()
                .serviceBus(serviceBusProperties)
                .jsonMessageHandler(messageHandler, ${formatter.payloadClassName}.class, ${sessionEnabled})
                .build();
    }
""")
        }
    }

    private void addConsumerImports(File configFile) {
        FileUtils.addImport(configFile, "org.springframework.beans.factory.annotation.Qualifier")
        FileUtils.addImport(configFile, "com.blackbaud.azure.servicebus.config.ServiceBusProperties")
        FileUtils.addImport(configFile, "com.blackbaud.azure.servicebus.consumer.ServiceBusConsumer")
        FileUtils.addImport(configFile, "com.blackbaud.azure.servicebus.consumer.ServiceBusConsumerBuilder")
    }

    private static class ServiceBusNameResolver {
        String topicNameCamelCase
        String topicNameSnakeCase

        ServiceBusNameResolver(String topicName) {
            if (topicName.contains("_")) {
                topicNameSnakeCase = topicName
                topicNameCamelCase = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, topicName)
            } else {
                if (Character.isLowerCase(topicName.charAt(0))) {
                    topicNameCamelCase = topicName
                    topicNameSnakeCase = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, topicName)
                } else {
                    topicNameCamelCase = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_CAMEL, topicName)
                    topicNameSnakeCase = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, topicName)
                }
            }
        }

        String getMessageHandlerClassName() {
            "${topicNameCamelCase.capitalize()}MessageHandler"
        }

        String getPayloadClassName() {
            "${topicNameCamelCase.capitalize()}Payload"
        }

        String getPropertiesClassName() {
            "${topicNameCamelCase.capitalize()}ServiceBusProperties"
        }

    }


    enum TopicType {
        SCHEDULE("schedulingTopicServiceBus"), DATASYNC("dataSyncTopicServiceBus")

        private String consumerBuilderMethodName

        private TopicType(String consumerBuilderMethodName) {
            this.consumerBuilderMethodName = consumerBuilderMethodName
        }

        String getConsumerBuilderMethodName() {
            return consumerBuilderMethodName
        }

        static TopicType resolveFromString(String name) {
            TopicType topicType = values().find {
                name.equalsIgnoreCase(it.name())
            }
            if (topicType == null) {
                List<String> types = values().collect{it.name().toLowerCase()}
                throw new RuntimeException("No type matching '${name}', available types=${types}")
            }
            topicType
        }
    }

}
