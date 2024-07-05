> [!NOTE]  
> This repos has been archived since LocalStack has been adjusted to this "CBOR dialect" in [localstack/localstack#11133](https://github.com/localstack/localstack/pull/11133).

# Kinesis CBOR handling reproducer
This repo contains a small reproducer to showcase the (non-standard-conform) CBOR timestamp handling in AWS Kinesis.

### Prerequisites
- LocalStack (if run locally)
- `awslocal` (if run locally)
- Maven
- Java 11

### Steps to reproduce
- Start localstack: `localstack start`
- Create a new kinesis stream: `awslocal kinesis create-stream --stream-name test-stream`
- Make sure to set `JAVA_HOME` to Java 11: `export JAVA_HOME="/usr/lib/jvm/java-1.11.0-openjdk-amd64/"`
- Start the Apache Flink consumer (which uses the AWS SDK under the hood):
  - With CBOR enabled (default): `mvn clean compile exec:java`
  - With CBOR disabled (use JSON instead): `AWS_CBOR_DISABLE=1 mvn clean compile exec:java`

The same sample can be run against AWS by using real-world credentials, removing the `AWS_ENDPOINT` config, and using `aws` instead of `awslocal`.

### Results
- AWS Kinesis uses a non-standard-conform timestamp encoding when using CBOR: It uses milliseconds instead of seconds.
- This issue has already been raised in https://github.com/aws/aws-sdk-java-v2/issues/4661.
- This specific handling is being addressed in LocalStack with https://github.com/localstack/localstack/pull/11133.
- When using the (non-default) JSON encoding with the AWS SDK, the timestamp handling differs.
  - In this case the timestamps have to be encoded as seconds.
  - [However, the AWS SDK always multiplies the timestamps with 1000](https://github.com/a0x8o/flink/blob/06e09c266a9cd80b27fc77d415cbd014da01839c/flink-connectors/flink-connector-kinesis/src/main/java/org/apache/flink/streaming/connectors/kinesis/util/KinesisConfigUtil.java#L538).
  - -> If `AWS_CBOR_DISABLE` is set (no matter the value), the timestamps have to be again divided by 1000-
