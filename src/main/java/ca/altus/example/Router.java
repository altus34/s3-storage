package ca.altus.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class Router {
    @Bean
    public FileController fileController(S3AsyncClient s3Client, S3ClientProperties properties) {
        return new FileController(s3Client, properties.bucket());
    }

    @Bean
    public RouterFunction<ServerResponse> routes(FileController fileController) {
        return route(PUT("/files"), fileController::save);
    }

}
