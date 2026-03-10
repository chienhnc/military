package com.military.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

  @Bean
  public S3Client s3Client(@Value("${military.app.s3.region:${AWS_REGION:ap-southeast-1}}") String region) {
    return S3Client.builder()
        .region(Region.of(region))
        .build();
  }
}
