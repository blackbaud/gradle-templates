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

    private void initEventHubsIfNotAlreadyInitialized() {
        if (basicProject.getBuildFile().text =~ /common-async-event-hubs/) {
            return
        }

        FileUtils.appendAfterLastLine(basicProject.getBuildFile(), /ext \{/,
                '        commonAsyncVersion = "2.+"')
        FileUtils.appendAfterLine(basicProject.getBuildFile(), /compile.*common-spring-boot/,
                '    compile "com.blackbaud:common-async-event-hubs:${commonAsyncVersion}"')
        FileUtils.appendAfterLine(basicProject.getBuildFile(), /sharedTestCompile/,
                '    sharedTestCompile "com.blackbaud:common-async-event-hubs-test:${commonAsyncVersion}"')

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application-local.properties")
        if ((applicationPropertiesFile.exists() && applicationPropertiesFile.text.contains("eventhubs.stub")) == false) {
            applicationPropertiesFile.append("""
eventhubs.stub=true
""")
        }

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/eventhubs") {
            "EventHubsConfig.java" template: "/templates/springboot/eventhubs/event-hubs-config.java.tmpl",
                                   servicePackageName: "${servicePackage}.servicebus"
        }
    }

    void addInternalEventHub(String eventHubName) {
        addEventHub(eventHubName, true, true, true)
    }

    void addExternalEventHub(String eventHubName, boolean consumer, boolean publisher) {
        addEventHub(eventHubName, false, consumer, publisher)
    }

    private void addEventHub(String eventHubName, boolean internal, boolean consumer, boolean publisher) {
        initEventHubsIfNotAlreadyInitialized()

        NameResolver formatter = new NameResolver(eventHubName)
        File componentTestConfigFile = basicProject.findComponentTestConfig()

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application-local.properties")
        if (applicationPropertiesFile.text.contains("eventhubs.namespace") == false) {
            applicationPropertiesFile.append("""
eventhubs.namespace=namespace
eventhubs.operationTimeout=60
""")
        }
        applicationPropertiesFile.append("""\
eventhubs.${formatter.nameSnakeCase}.name=${formatter.nameSnakeCase}
eventhubs.${formatter.nameSnakeCase}.shared_access_key_name=keyName
eventhubs.${formatter.nameSnakeCase}.shared_access_key=key
""")
        if (consumer) {
            applicationPropertiesFile.append("""\
eventhubs.${formatter.nameSnakeCase}.consumer.storageAccountContainer=container
""")
        }

        File eventHubsConfigFile = basicProject.findFile("EventHubsConfig.java")
        FileUtils.appendToClass(eventHubsConfigFile, """
    @Bean
    public EventHubsProperties ${formatter.nameCamelCase}EventHubsProperties(
            @Value("\${eventhubs.namespace}") String namespace,
            @Value("\${eventhubs.${formatter.nameSnakeCase}.name}") String eventHubName,
            @Value("\${eventhubs.${formatter.nameSnakeCase}.shared_access_key_name}") String sasKeyName,
            @Value("\${eventhubs.${formatter.nameSnakeCase}.shared_access_key}") String sasKey,
            @Value("\${eventhubs.${formatter.nameSnakeCase}.storage_account_container}") String storageAccountContainer,
            @Value("\${eventhubs.operationTimeout}") int operationTimeout) {
        return EventHubsProperties.builder()
                .hubName(eventHubName)
                .namespace(eventHubNamespace)
                .sasKey(sasKey)
                .sasKeyName(sasKeyName)
                .storageAccountContainer(storageAccountContainer)
                .operationTimeout(operationTimeout)
                .build();
    }
    
""")

        File publisherConfigFile

        if (publisher) {
            publisherConfigFile = eventHubsConfigFile
            if (internal) {
                basicProject.addInternalApiObject("event-hubs", formatter.payloadClassName, false)
            } else {
                basicProject.addExternalApiObject("event-hubs", formatter.payloadClassName, false)
            }
        } else {
            publisherConfigFile = componentTestConfigFile
        }

        FileUtils.addImport(publisherConfigFile, "org.springframework.beans.factory.annotation.Qualifier")
        FileUtils.addImport(publisherConfigFile, "com.blackbaud.azure.eventhubs.publisher.JsonMessagePublisher")
        FileUtils.addImport(publisherConfigFile, "com.blackbaud.azure.eventhubs.config.EventHubsProperties")
        FileUtils.addImport(publisherConfigFile, "com.blackbaud.azure.eventhubs.publisher.EventHubPublisherFactory")
        FileUtils.appendToClass(publisherConfigFile, """
    @Bean
    public JsonMessagePublisher ${formatter.nameCamelCase}Publisher(
            EventHubPublisherFactory eventHubsPublisherFactory,
            @Qualifier("${formatter.nameCamelCase}EventHubsProperties") EventHubsProperties eventHubsProperties) {
        return eventHubsPublisherFactory.createPartitionedJsonPublisher(eventHubsProperties);
    }
""")

        if (consumer) {
            basicProject.applyTemplate("src/main/java/${servicePackagePath}/eventhubs") {
                "${formatter.messageHandlerClassName}.java" template: "/templates/springboot/eventhubs/event-batch-handler.java.tmpl",
                                                            servicePackageName: "${servicePackage}.eventhubs",
                                                            className: formatter.eventHandlerClassName,
                                                            payloadClassName: formatter.payloadClassName
            }

            addServiceBusConsumerImports(eventHubsConfigFile)
            FileUtils.appendToClass(eventHubsConfigFile, """
    @Bean
    public ${formatter.eventHandlerClassName} ${formatter.nameCamelCase}EventHandler() {
        return new ${formatter.eventHandlerClassName}();
    }

    @Bean
    public EventHubConsumer ${formatter.nameCamelCase}Consumer(
            EventHubConsumerBuilder.Factory eventHubConsumerFactory,
            ${formatter.eventHandlerClassName} ${formatter.nameCamelCase}EventHandler,
            @Qualifier("${formatter.nameCamelCase}ServiceBusProperties") EventHubsProperties eventHubsProperties) {
        return eventHubConsumerFactory.create()
                .eventHub(eventHubsProperties)
                .jsonEventBatchHandler(${formatter.nameCamelCase}EventHandler, ${formatter.payloadClassName}.class)
                .build();
    }
""")
        } else {
            addEventHubsConsumerImports(componentTestConfigFile)
            FileUtils.addImport(componentTestConfigFile, "com.blackbaud.azure.eventhubs.consumer.handlers.ValidatingEventBatchHandler")
            FileUtils.appendToClass(componentTestConfigFile, """
    @Bean
    public ValidatingEventBatchHandler<${formatter.payloadClassName}> ${formatter.eventHandlerClassName}() {
        return new ValidatingEventBatchHandler<>("${formatter.nameCamelCase}Handler");
    }

    @Bean
    public EventHubConsumer ${formatter.nameCamelCase}Consumer(
            EventHubConsumerBuilder.Factory eventHubConsumerFactory,
            @Qualifier("${formatter.nameCamelCase}MessageHandler") ValidatingEventBatchHandler<${formatter.payloadClassName}> eventHandler,
            @Qualifier("${formatter.nameCamelCase}EventHubsProperties") EventHubsProperties eventHubsProperties) {
        return eventHubConsumerFactory.create()
                .eventHub(eventHubsProperties)
                .jsonEventBatchHandler(messageHandler, ${formatter.payloadClassName}.class)
                .build();
    }
""")
        }
    }

    private void addEventHubsConsumerImports(File configFile) {
        FileUtils.addImport(configFile, "org.springframework.beans.factory.annotation.Qualifier")
        FileUtils.addImport(configFile, "com.blackbaud.azure.eventhubs.adapter.EventHubsProperties")
        FileUtils.addImport(configFile, "com.blackbaud.azure.eventhubs.consumer.EventHubConsumer")
        FileUtils.addImport(configFile, "com.blackbaud.azure.eventhubs.consumer.EventHubConsumerBuilder")
    }



    private void initServiceBusIfNotAlreadyInitialized() {
        if (basicProject.getBuildFile().text =~ /common-async-service-bus/) {
            return
        }

        FileUtils.appendAfterLastLine(basicProject.getBuildFile(), /ext \{/,
                '        commonAsyncVersion = "2.+"')
        FileUtils.appendAfterLine(basicProject.getBuildFile(), /compile.*common-spring-boot/,
                '    compile "com.blackbaud:common-async-service-bus:${commonAsyncVersion}"')
        FileUtils.appendAfterLine(basicProject.getBuildFile(), /sharedTestCompile/,
                '    sharedTestCompile "com.blackbaud:common-async-service-bus-test:${commonAsyncVersion}"')

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application-local.properties")
        if ((applicationPropertiesFile.exists() && applicationPropertiesFile.text.contains("servicebus.stub")) == false) {
            applicationPropertiesFile.append("""
servicebus.stub=true
""")
        }

        basicProject.applyTemplate("src/main/java/${servicePackagePath}/servicebus") {
            "ServiceBusConfig.java" template: "/templates/springboot/service-bus/service-bus-config.java.tmpl",
                                    servicePackageName: "${servicePackage}.servicebus"
        }
    }

    void addInternalTopic(String topicName, boolean sessionEnabled) {
        addTopic(topicName, true, true, true, sessionEnabled)
    }

    void addExternalTopic(String topicName, boolean consumer, boolean publisher, boolean sessionEnabled) {
        addTopic(topicName, false, consumer, publisher, sessionEnabled)
    }

    private void addTopic(String topicName, boolean internal, boolean consumer, boolean publisher, boolean sessionEnabled) {
        initServiceBusIfNotAlreadyInitialized()

        NameResolver formatter = new NameResolver(topicName)
        File componentTestConfigFile = basicProject.findComponentTestConfig()

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application-local.properties")
        if (applicationPropertiesFile.text.contains("servicebus.namespace") == false) {
            applicationPropertiesFile.append("""
servicebus.namespace=namespace
""")
        }
        applicationPropertiesFile.append("""\
servicebus.${formatter.nameSnakeCase}.entity_path=${formatter.nameSnakeCase}
servicebus.${formatter.nameSnakeCase}.shared_access_key_name=keyName
servicebus.${formatter.nameSnakeCase}.shared_access_key=key
""")
        if (consumer) {
            applicationPropertiesFile.append("""\
servicebus.${formatter.nameSnakeCase}.subscription=consumer
""")
        }

        File serviceBusConfigFile = basicProject.findFile("ServiceBusConfig.java")
        FileUtils.appendToClass(serviceBusConfigFile, """
    @Bean
    public ServiceBusProperties ${formatter.nameCamelCase}ServiceBusProperties(
            @Value("\${servicebus.namespace}") String namespace,
            @Value("\${servicebus.${formatter.nameSnakeCase}.entity_path}") String entityPath,
            @Value("\${servicebus.${formatter.nameSnakeCase}.subscription}") String subscription,
            @Value("\${servicebus.${formatter.nameSnakeCase}.shared_access_key_name}") String sasKeyName,
            @Value("\${servicebus.${formatter.nameSnakeCase}.shared_access_key}") String sasKey) {
        return ServiceBusProperties.builder()
                .namespace(namespace)
                .entityPath(entityPath)
                .subscription(subscription)
                .sharedAccessKey(sasKey)
                .sharedAccessKeyName(sasKeyName)
                .build();
    }
""")

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
        }

        FileUtils.addImport(publisherConfigFile, "org.springframework.beans.factory.annotation.Qualifier")
        FileUtils.addImport(publisherConfigFile, "com.blackbaud.azure.servicebus.publisher.JsonMessagePublisher")
        FileUtils.addImport(publisherConfigFile, "com.blackbaud.azure.servicebus.adapter.ServiceBusProperties")
        FileUtils.addImport(publisherConfigFile, "com.blackbaud.azure.servicebus.publisher.ServiceBusPublisherFactory")
        FileUtils.appendToClass(publisherConfigFile, """
    @Bean
    public JsonMessagePublisher ${formatter.nameCamelCase}Publisher(
            ServiceBusPublisherFactory serviceBusPublisherFactory,
            @Qualifier("${formatter.nameCamelCase}ServiceBusProperties") ServiceBusProperties serviceBusProperties) {
        return serviceBusPublisherFactory.createJsonPublisher(serviceBusProperties);
    }
""")

        if (consumer) {
            basicProject.applyTemplate("src/main/java/${servicePackagePath}/servicebus") {
                "${formatter.messageHandlerClassName}.java" template: "/templates/springboot/service-bus/message-handler.java.tmpl",
                                                            servicePackageName: "${servicePackage}.servicebus",
                                                            className: formatter.messageHandlerClassName,
                                                            payloadClassName: formatter.payloadClassName
            }

            addServiceBusConsumerImports(serviceBusConfigFile)
            FileUtils.appendToClass(serviceBusConfigFile, """
    @Bean
    public ${formatter.messageHandlerClassName} ${formatter.nameCamelCase}MessageHandler() {
        return new ${formatter.messageHandlerClassName}();
    }

    @Bean
    public ServiceBusConsumer ${formatter.nameCamelCase}Consumer(
            ServiceBusConsumerBuilder.Factory serviceBusConsumerFactory,
            ${formatter.messageHandlerClassName} ${formatter.nameCamelCase}MessageHandler,
            @Qualifier("${formatter.nameCamelCase}ServiceBusProperties") ServiceBusProperties serviceBusProperties) {
        return serviceBusConsumerFactory.create()
                .serviceBus(serviceBusProperties)
                .jsonMessageHandler(${formatter.nameCamelCase}MessageHandler, ${formatter.payloadClassName}.class, ${sessionEnabled})
                .build();
    }
""")
        } else {
            addServiceBusConsumerImports(componentTestConfigFile)
            FileUtils.addImport(componentTestConfigFile, "com.blackbaud.azure.servicebus.consumer.handlers.ValidatingServiceBusMessageHandler")
            FileUtils.appendToClass(componentTestConfigFile, """
    @Bean
    public ValidatingServiceBusMessageHandler<${formatter.payloadClassName}> ${formatter.messageHandlerClassName}() {
        return new ValidatingServiceBusMessageHandler<>("${formatter.nameCamelCase}Handler");
    }

    @Bean
    public ServiceBusConsumer sessionConsumer(
            ServiceBusConsumerBuilder.Factory serviceBusConsumerFactory,
            @Qualifier("${formatter.nameCamelCase}MessageHandler") ValidatingServiceBusMessageHandler<${formatter.payloadClassName}> messageHandler,
            @Qualifier("${formatter.nameCamelCase}ServiceBusProperties") ServiceBusProperties serviceBusProperties) {
        return serviceBusConsumerFactory.create()
                .serviceBus(serviceBusProperties)
                .jsonMessageHandler(messageHandler, ${formatter.payloadClassName}.class, ${sessionEnabled})
                .build();
    }
""")
        }
    }

    private void addServiceBusConsumerImports(File configFile) {
        FileUtils.addImport(configFile, "org.springframework.beans.factory.annotation.Qualifier")
        FileUtils.addImport(configFile, "com.blackbaud.azure.servicebus.adapter.ServiceBusProperties")
        FileUtils.addImport(configFile, "com.blackbaud.azure.servicebus.consumer.ServiceBusConsumer")
        FileUtils.addImport(configFile, "com.blackbaud.azure.servicebus.consumer.ServiceBusConsumerBuilder")
    }

    private static class NameResolver {
        String nameCamelCase
        String nameSnakeCase

        NameResolver(String topicName) {
            if (topicName.contains("_")) {
                nameSnakeCase = topicName
                nameCamelCase = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, topicName)
            } else {
                if (Character.isLowerCase(topicName.charAt(0))) {
                    nameCamelCase = topicName
                    nameSnakeCase = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, topicName)
                } else {
                    nameCamelCase = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_CAMEL, topicName)
                    nameSnakeCase = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, topicName)
                }
            }
        }

        String getEventHandlerClassName() {
            "${nameCamelCase.capitalize()}EventHandler"
        }

        String getMessageHandlerClassName() {
            "${nameCamelCase.capitalize()}MessageHandler"
        }

        String getPayloadClassName() {
            "${nameCamelCase.capitalize()}Payload"
        }

    }

}
