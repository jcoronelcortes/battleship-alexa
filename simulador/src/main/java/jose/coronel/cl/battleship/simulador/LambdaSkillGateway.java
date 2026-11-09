package jose.coronel.cl.battleship.simulador;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/** The skill deployed on AWS Lambda, invoked with your local AWS credentials (aws login). */
public class LambdaSkillGateway implements SkillGateway {

    private final LambdaClient lambda;
    private final String functionName;

    public LambdaSkillGateway(String region, String functionName) {
        this.lambda = LambdaClient.builder().region(Region.of(region)).build();
        this.functionName = functionName;
    }

    @Override
    public String send(String requestJson) {
        InvokeResponse response = lambda.invoke(InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(requestJson))
                .build());
        String body = response.payload().asUtf8String();
        if (response.functionError() != null) {
            throw new IllegalStateException("La Lambda devolvió un error: " + body);
        }
        return body;
    }

}
