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
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

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

    private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:1.3");

    private WebClient webClient;

    @LocalServerPort
    private int port;

    @Autowired
    private S3AsyncClient s3Client;

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
        webClient = WebClient.create("http://localhost:" + port);
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
        assertThat(listObjectsIn("test-bucket"), empty());

        Mono<ClientResponse> downloadFile = webClient.put()
                .uri("/files")
                .contentType(APPLICATION_OCTET_STREAM)
                .body(fromResource(new ClassPathResource("test_file_2.txt")))
                .exchangeToMono(Mono::just);

        StepVerifier.create(downloadFile)
                .assertNext(response -> {
                    assertThat(response.statusCode().value(), is(201));
                    assertThat(response.headers().header(LOCATION).get(0), matchesPattern("http://localhost:%s/files/%s".formatted(port, UUID_STRING)));

                    final String fileName = response.headers().header(LOCATION).get(0).split("files/")[1];
                    List<S3Object> s3Objects = listObjectsIn("test-bucket");
                    assertThat(s3Objects, hasSize(1));
                    assertThat(s3Objects.get(0).key(), equalTo(fileName));
                })
                .then(() -> assertThat(listObjectsIn("test-bucket"), hasSize(1)))
                .verifyComplete();
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

    private List<S3Object> listObjectsIn(String bucket) {
        try {
            return s3Client.listObjects(ListObjectsRequest.builder().bucket(bucket).build()).get().contents();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
