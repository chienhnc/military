package com.military.repository.dynamodb;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

final class DynamoIdGenerator {
  private DynamoIdGenerator() {
  }

  static long nextId() {
    long epochPart = Instant.now().toEpochMilli() % 1_000_000_000_000L;
    long randomPart = ThreadLocalRandom.current().nextLong(1_000L, 10_000L);
    return (epochPart * 10_000L) + randomPart;
  }
}
