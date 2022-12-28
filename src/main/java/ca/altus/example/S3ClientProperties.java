package ca.altus.example;

import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.regions.Region;

@ConfigurationProperties(prefix = "aws.s3")
public record S3ClientProperties(Region region, String accessKey, String secretKey, String bucket) {
}
