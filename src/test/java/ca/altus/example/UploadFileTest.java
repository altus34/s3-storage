package ca.altus.example;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;


@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class UploadFileTest {
    static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:1.3");

    private static S3Client s3;

    @Autowired
    private S3AsyncClient s3client;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ResourceLoader resourceLoader;

    @Container
    public static LocalStackContainer localstack = new LocalStackContainer(localstackImage).withServices(S3);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("AWS_REGION", localstack::getRegion);
        registry.add("AWS_ACCESS_KEY", localstack::getAccessKey);
        registry.add("AWS_SECRET_KEY", localstack::getSecretKey);
        registry.add("AWS_BUCKET_NAME", () -> "test-bucket");
    }

    @BeforeAll
    public static void setUp() {
        s3 = S3Client
                .builder()
                .endpointOverride(localstack.getEndpointOverride(S3))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                        )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

        s3.createBucket(CreateBucketRequest.builder().bucket("test-bucket").build());
    }

    @Test
    @DisplayName("Should upload file in S3 bucket")
    void uploadFile() throws Exception {
        // see https://www.baeldung.com/java-aws-s3-reactive
        assertThat(s3.listBuckets().buckets(), hasSize(1));

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new ClassPathResource("test_file_2.txt"))
                .contentType(MULTIPART_FORM_DATA);

        webTestClient.put()
                .uri("/file")
                .body(fromMultipartData(multipartBodyBuilder.build()))
                .exchange()
                .expectStatus()
                .isOk();

//        assertThat(s3.listBuckets().buckets(), hasSize(1));


    }
}
