package cloud.localstack.flink;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer;
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants;

import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        SourceFunction<String> source = new FlinkKinesisConsumer<>("test-stream", new SimpleStringSchema(), getProperties());
        DataStream<String> stream = env.addSource(source);
        stream.addSink(new PrintSinkFunction<>()).name("print-sink");
        env.execute("Kinesis Flink Application");
    }

    private static Properties getProperties() {
        Properties inputProperties = new Properties();
        inputProperties.setProperty(ConsumerConfigConstants.AWS_ENDPOINT, "https://localhost.localstack.cloud:4566");
        inputProperties.setProperty(ConsumerConfigConstants.AWS_ACCESS_KEY_ID, "test");
        inputProperties.setProperty(ConsumerConfigConstants.AWS_SECRET_ACCESS_KEY, "test");

        inputProperties.setProperty(ConsumerConfigConstants.AWS_REGION, "us-east-1");
        inputProperties.setProperty(ConsumerConfigConstants.STREAM_INITIAL_POSITION, "AT_TIMESTAMP");

        String cborDisabledEnv = System.getenv().get("AWS_CBOR_DISABLE");
        // CBOR is disabled as soon as the environment variable is set (no matter the value)
        boolean isCbor = cborDisabledEnv == null;
        if (isCbor) {
            // CBOR / AWS_CBOR_DISABLE NOT PRESENT (also not 0) -> Verified against AWS
            // NOT WORKING FOR JSON / AWS_CBOR_DISABLE=1 -> Verified against AWS that it leads to an error:
            //    InvalidArgumentException: The timestampInMillis parameter cannot be greater than the currentTimestampInMillis.
            inputProperties.setProperty(ConsumerConfigConstants.STREAM_INITIAL_TIMESTAMP, "1719990654.365");
        } else {
            // JSON / AWS_CBOR_DISABLE=1 -> Verified against AWS
            // CBOR / AWS_CBOR_DISABLE NOT PRESENT (also not 0) -> Verified against AWS (but just works because it's in the past)
            // In this case we have to divide the unixtimestamp by 1000, because the SDK multiplies it by 1000 before serializing it:
            // - https://github.com/a0x8o/flink/blob/06e09c266a9cd80b27fc77d415cbd014da01839c/flink-connectors/flink-connector-kinesis/src/main/java/org/apache/flink/streaming/connectors/kinesis/util/KinesisConfigUtil.java#L538
            inputProperties.setProperty(ConsumerConfigConstants.STREAM_INITIAL_TIMESTAMP, "1719990.654365");
        }
        return inputProperties;
    }
}