package com.military.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

  @Bean
  public DynamoDbClient dynamoDbClient(
      @Value("${military.app.dynamodb.region:${AWS_REGION:ap-southeast-1}}") String region,
      @Value("${military.app.dynamodb.endpoint:}") String endpoint) {
    DynamoDbClientBuilder builder = DynamoDbClient.builder()
        .region(Region.of(region));
    if (StringUtils.hasText(endpoint)) {
      builder.endpointOverride(URI.create(endpoint));
    }
    return builder.build();
  }

  @Bean
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
    return DynamoDbEnhancedClient.builder()
        .dynamoDbClient(dynamoDbClient)
        .build();
  }
}
