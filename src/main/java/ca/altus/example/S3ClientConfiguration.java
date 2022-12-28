package ca.altus.example;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(S3ClientProperties.class)
public class S3ClientConfiguration {

    @Bean
    public S3AsyncClient s3client(S3ClientProperties config, AwsCredentialsProvider credentialsProvider) {
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .writeTimeout(Duration.ZERO)
                .maxConcurrency(64)
                .build();

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .checksumValidationEnabled(false)
                .chunkedEncodingEnabled(true)
                .build();

        S3AsyncClientBuilder b = S3AsyncClient.builder()
                .httpClient(httpClient)
                .region(config.region())
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(serviceConfiguration);

        return b.build();
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(S3ClientProperties config) {
        return () -> (AwsCredentials) AwsBasicCredentials.create(config.accessKey(), config.secretKey());
    }
}
