package com.blackbaud.service.core.domain;

import org.bson.types.ObjectId;
import com.blackbaud.cosmos.sharded.ShardedMongoRepository;

public interface TruckTransactionalRepository extends ShardedMongoRepository<TruckEntity, ObjectId> {
}
