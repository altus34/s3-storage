# s3-storage
Use Spring Cloud Gateway to save Multipart Doc to S3 bucket


## S3

aws s3 ls s3://test-bucket --endpoint-url http://127.0.0.1:32882

aws s3 cp s3://test-bucket/349bbc26-a306-475f-9aaf-34e3a2735643 .toto.txt --endpoint-url http://127.0.0.1:32882