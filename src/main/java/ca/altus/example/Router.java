package ca.altus.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class Router {
    @Bean
    public FileController fileController() {
        return new FileController();
    }
    @Bean
    public RouterFunction<ServerResponse> routes(FileController fileController) {
        return route(PUT("/file").and(contentType(MULTIPART_FORM_DATA)), fileController::save);
    }

}
