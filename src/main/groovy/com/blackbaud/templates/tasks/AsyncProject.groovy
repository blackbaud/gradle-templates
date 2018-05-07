package com.blackbaud.templates.tasks

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
                '        commonAsyncServiceBusVersion = "2.+"')
        FileUtils.appendAfterLine(basicProject.getBuildFile(), /compile.*common-spring-boot/,
                '    compile "com.blackbaud:common-async-service-bus:${commonAsyncServiceBusVersion}"')
        FileUtils.appendAfterLine(basicProject.getBuildFile(), /sharedTestCompile/,
                '    sharedTestCompile "com.blackbaud:common-async-service-bus-test:${commonAsyncServiceBusVersion}"')

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application-local.properties")
        applicationPropertiesFile.append("""
servicebus.stub=true
""")

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/servicebus") {
            "ServiceBusConfig.java" template: "/templates/springboot/servicebus/service-bus-config.java.tmpl",
                                    servicePackageName: "${servicePackage}.servicebus"
        }
    }

    void addInternalTopic(String topicName, boolean sessionEnabled) {
        addTopic(topicName, true, true, true, sessionEnabled)
    }

    void addExternalTopic(String topicName, boolean consumer, boolean publisher, boolean sessionEnabled) {
        addTopic(topicName, false, publisher, consumer, sessionEnabled)
    }

    private void addTopic(String topicName, boolean internal, boolean consumer, boolean publisher, boolean sessionEnabled) {
        initServiceBusIfNotAlreadyInitialized()

        ServiceBusNameResolver formatter = new ServiceBusNameResolver(topicName)

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application-local.properties")
        if (applicationPropertiesFile.text.contains("servicebus.namespace") == false) {
            applicationPropertiesFile.append("""
    servicebus.namespace=namespace
""")
        }
        applicationPropertiesFile.append("""
servicebus.${formatter.topicNameSnakeCase}.entity_path=${formatter.topicNameSnakeCase}
servicebus.${formatter.topicNameSnakeCase}.shared_access_key_name=keyName
servicebus.${formatter.topicNameSnakeCase}.shared_access_key=key
""")
        if (consumer) {
            applicationPropertiesFile.append("""
servicebus.${formatter.topicNameSnakeCase}.subscription=consumer
""")
        }

        File serviceBusConfigFile = basicProject.findFile("ServiceBusConfig.java")
        FileUtils.appendToClass(serviceBusConfigFile, """
    @Bean
    public ServiceBusProperties ${formatter.topicNameCamelCase}ServiceBusProperties(
            @Value("\${servicebus.namespace}") String namespace,
            @Value("\${servicebus.${formatter.topicNameSnakeCase}.entity_path}") String entityPath,
            @Value("\${servicebus.${formatter.topicNameSnakeCase}.subscription}") String subscription,
            @Value("\${servicebus.${formatter.topicNameSnakeCase}.shared_access_key_name}") String sasKeyName,
            @Value("\${servicebus.${formatter.topicNameSnakeCase}.shared_access_key}") String sasKey) {
        return ServiceBusProperties.builder()
                .namespace(namespace)
                .entityPath(entityPath)
                .subscription(subscription)
                .sharedAccessKey(sasKey)
                .sharedAccessKeyName(sasKeyName)
                .build();
    }
""")

        File publisherConfigFile = publisher ? serviceBusConfigFile : basicProject.findComponentTestConfig()
        FileUtils.appendToClass(publisherConfigFile, """
    @Bean
    public JsonMessagePublisher ${formatter.topicNameCamelCase}Publisher(
            ServiceBusPublisherFactory serviceBusPublisherFactory,
            @Qualifier("${formatter.topicNameCamelCase}ServiceBusProperties") ServiceBusProperties serviceBusProperties) {
        return serviceBusPublisherFactory.createJsonPublisher(serviceBusProperties);
    }
""")

        if (consumer) {
            if (internal) {
                basicProject.applyTemplate("src/main/java/${servicePackagePath}/servicebus") {
                    "${formatter.payloadClassName}.java" template: "/templates/springboot/servicebus/payload.java.tmpl",
                                                         servicePackageName: "${servicePackage}.servicebus",
                                                         className: formatter.payloadClassName
                }
            }

            basicProject.applyTemplate("src/main/java/${servicePackagePath}/servicebus") {
                "${formatter.messageHandlerClassName}.java" template: "/templates/springboot/servicebus/message-handler.java.tmpl",
                                                            servicePackageName: "${servicePackage}.servicebus",
                                                            className: formatter.messageHandlerClassName,
                                                            payloadClassName: formatter.payloadClassName
            }

            FileUtils.appendToClass(serviceBusConfigFile, """
    @Bean
    public ${formatter.messageHandlerClassName} ${formatter.topicNameCamelCase}MessageHandler() {
        return new ${formatter.messageHandlerClassName}();
    }

    @Bean
    public ServiceBusConsumer ${formatter.topicNameCamelCase}Consumer(
            ServiceBusConsumerBuilder.Factory serviceBusConsumerFactory,
            ${formatter.messageHandlerClassName} ${formatter.topicNameCamelCase}MessageHandler,
            @Qualifier("${formatter.topicNameCamelCase}ServiceBusProperties") ServiceBusProperties serviceBusProperties) {
        return serviceBusConsumerFactory.create()
                .serviceBus(serviceBusProperties)
                .jsonMessageHandler(${formatter.topicNameCamelCase}MessageHandler, ${formatter.payloadClassName}.class, ${sessionEnabled})
                .build();
    }
""")
        } else {
            File configFile = basicProject.findComponentTestConfig()
            FileUtils.appendToClass(configFile, """
    @Bean
    public ValidatingServiceBusMessageHandler<${formatter.payloadClassName}> ${formatter.messageHandlerClassName}() {
        return new ValidatingServiceBusMessageHandler<>("${formatter.topicNameCamelCase}Handler");
    }

    @Bean
    public ServiceBusConsumer sessionConsumer(
            ServiceBusConsumerBuilder.Factory serviceBusConsumerFactory,
            @Qualifier("${formatter.topicNameCamelCase}MessageHandler") ValidatingServiceBusMessageHandler<{$formatter.payloadClassName}> messageHandler,
            @Qualifier("${formatter.topicNameCamelCase}ServiceBusProperties") ServiceBusProperties serviceBusProperties) {
        return serviceBusConsumerFactory.create()
                .serviceBus(serviceBusProperties)
                .jsonMessageHandler(messageHandler, ${formatter.payloadClassName}.class, ${sessionEnabled})
                .build();
    }
""")
        }
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

    }

}
