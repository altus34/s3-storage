package ca.altus.example;

import software.amazon.awssdk.core.SdkResponse;

public class S3Exception extends RuntimeException {
    public S3Exception(SdkResponse response) {
    }
}
