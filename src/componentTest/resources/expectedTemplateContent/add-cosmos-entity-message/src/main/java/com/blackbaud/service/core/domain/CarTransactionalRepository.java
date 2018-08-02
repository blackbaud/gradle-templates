package com.blackbaud.service.core.domain;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface CarTransactionalRepository extends ShardedMongoRepository<CarEntity, ObjectId> {
}
