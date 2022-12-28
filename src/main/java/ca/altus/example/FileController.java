package ca.altus.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.web.reactive.function.BodyExtractors.toMultipartData;

public class FileController {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);


    public Mono<ServerResponse> save(ServerRequest request) {
        return request.body(toMultipartData())
                .flatMapMany(parts -> parts.toSingleValueMap().get("file").content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    LOGGER.info("===> Data Buffer: {}", new String(bytes, UTF_8));
                    return Mono.empty();
                }).then(ServerResponse.ok().build());

//                        .flatMap(dataBuffer -> {
//                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
//                    dataBuffer.read(bytes);
//                    DataBufferUtils.release(dataBuffer);
//                    return new String(bytes, StandardCharsets.UTF_8);})
//                .flatMap(o -> {
//                        LOGGER.info("===> Data Buffer: {}", new String(bytes, UTF_8));
//                        return Mono.empty();
//                    });
//            return Mono.empty();
//        }).then(ServerResponse.ok().build());



//                .flatMap(dataBuffer -> {
//                    LOGGER.info("===> Data Buffer: {}", new String(dataBuffer.toByteBuffer().array()));
//                    return Mono.empty();
//                }).then(ServerResponse.ok().build());


//                .flatMap(parts -> {
//                    Map<String, Part> partMap = parts.toSingleValueMap();
//
//                    partMap.forEach((partName, value) -> {
//                        LOGGER.info("Name: {}, value: {}", partName, value);
//                    });
//
//                     Handle file
//                    FilePart file = (FilePart) partMap.get("file");
//
//                    LOGGER.info("File name: {}", image.filename());
//
                    // Handle profile
//                    FormFieldPart profile = (FormFieldPart) partMap.get("profile");
//                    return Mono.empty();
//                })
//                .then(ServerResponse.ok().build());

//        request.body(BodyExtractors.toMultipartData());
//        return ServerResponse.ok().build();
    }
}
