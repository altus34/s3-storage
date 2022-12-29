package ca.altus.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.web.reactive.function.server.ServerResponse.created;
import static org.springframework.web.util.UriComponentsBuilder.newInstance;
import static reactor.core.publisher.Mono.fromFuture;
import static reactor.core.publisher.Mono.just;

public class FileController {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    private final S3AsyncClient s3Client;
    private final String bucket;

    public FileController(S3AsyncClient s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        var fileKey = UUID.randomUUID().toString();
        var metadata = new HashMap<String, String>();

        var length = request.headers().contentLength().orElse(0);
        LOGGER.debug("LENGTH =====> {}", length);

        Flux<ByteBuffer> body = request.body(BodyExtractors.toDataBuffers())
                .flatMap(dataBuffer -> just(dataBuffer.toByteBuffer()));

        CompletableFuture<PutObjectResponse> future = s3Client
                .putObject(PutObjectRequest.builder()
                                .bucket(bucket)
                                .contentLength(length)
                                .key(fileKey)
                                .contentType(APPLICATION_OCTET_STREAM_VALUE)
                                .metadata(metadata)
                                .build(),
                        AsyncRequestBody.fromPublisher(body));

        return fromFuture(future)
                .flatMap((response) -> {
                    checkResult(response);
                    return created(newInstance().path("{uri}/{id}").buildAndExpand(request.uri(), fileKey).toUri()).build();
                });
    }

    private static void checkResult(SdkResponse result) {
        if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful()) {
            throw new S3Exception(result);
        }
    }
}
