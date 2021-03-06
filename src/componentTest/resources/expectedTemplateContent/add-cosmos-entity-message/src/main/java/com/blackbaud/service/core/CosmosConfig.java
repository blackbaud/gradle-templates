package com.blackbaud.service.core;

import com.blackbaud.cosmos.config.MongoCosmosConfig;
import com.blackbaud.cosmos.sharded.ShardedMongoRepositoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Arrays;
import java.util.Collection;

@Configuration
@EnableMongoRepositories(basePackages = CosmosConfig.REPOSITORY_AND_ENTITY_BASE_PACKAGE, repositoryBaseClass = ShardedMongoRepositoryImpl.class)
public class CosmosConfig extends MongoCosmosConfig {

    static final String REPOSITORY_AND_ENTITY_BASE_PACKAGE = "com.blackbaud.service.core.domain";

    @Value("${spring.data.mongodb.uri}")
    private String databaseUri;

    @Value("${spring.application.name}-db")
    private String databaseName;

    @Override
    public String getDatabaseUri() {
        return databaseUri;
    }

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    protected Collection<String> getMappingBasePackages() {
        return Arrays.asList(REPOSITORY_AND_ENTITY_BASE_PACKAGE);
    }

    @Bean
    public CarRepository carRepository(CosmosRetryableRepositoryFactory factory, CarTransactionalRepository transactionalRepository) {
        return factory.createRepository(transactionalRepository, CarRepository.class);
    }

}
