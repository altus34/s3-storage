package ca.altus.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.web.reactive.function.BodyInserters.fromResource;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class UploadFileTest {
    private static final String UUID_STRING = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

    static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:1.3");

    @LocalServerPort
    private int port;

    @Autowired
    private S3AsyncClient s3Client;

    @Autowired
    private WebTestClient webTestClient;

    @Container
    public static LocalStackContainer localstack = new LocalStackContainer(localstackImage).withServices(S3);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("AWS_REGION", localstack::getRegion);
        registry.add("AWS_ACCESS_KEY", localstack::getAccessKey);
        registry.add("AWS_SECRET_KEY", localstack::getSecretKey);
        registry.add("aws.s3.endpoint", () -> localstack.getEndpointOverride(S3));
        registry.add("AWS_BUCKET_NAME", () -> "test-bucket");
    }

    @BeforeEach
    void setUp() throws Exception {
        s3Client.createBucket(CreateBucketRequest.builder().bucket("test-bucket").build()).get();
    }

    @AfterEach
    void tearDown() throws Exception {
        deleteBucket("test-bucket");
    }

    @Test
    @DisplayName("Should upload file in S3 bucket")
    void uploadFile() throws Exception {
        // see https://www.baeldung.com/java-aws-s3-reactive
        ListObjectsRequest listObjects = ListObjectsRequest
                .builder()
                .bucket("test-bucket")
                .build();

        ListObjectsResponse res = s3Client.listObjects(listObjects).get();
        assertThat(res.contents(), empty());

        webTestClient.put()
                .uri("/files")
                .contentType(APPLICATION_OCTET_STREAM)
                .body(fromResource(new ClassPathResource("test_file_2.txt")))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().value(LOCATION, matchesPattern("http://localhost:%s/files/%s".formatted(port, UUID_STRING)));

        res = s3Client.listObjects(listObjects).get();
        assertThat(res.contents(), hasSize(1));
    }

    private void deleteBucket(String bucket) throws Exception {
        ListObjectsResponse objectListing = s3Client.listObjects(ListObjectsRequest.builder().bucket(bucket).build()).get();
        objectListing.contents().forEach(s3Object -> {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Object.key())
                    .build();
            try {
                s3Client.deleteObject(request).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket("test-bucket").build()).get();
    }
}
