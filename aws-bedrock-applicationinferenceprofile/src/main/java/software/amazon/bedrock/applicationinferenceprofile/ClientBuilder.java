package software.amazon.bedrock.applicationinferenceprofile;

import java.net.URI;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    public static BedrockClient getClient() {
        return BedrockClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
    }
}
