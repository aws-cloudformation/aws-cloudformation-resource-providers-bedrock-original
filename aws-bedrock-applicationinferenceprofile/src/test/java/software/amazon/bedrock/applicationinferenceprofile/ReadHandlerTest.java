package software.amazon.bedrock.applicationinferenceprofile;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileStatus;
import software.amazon.awssdk.services.bedrock.model.InternalServerException;
import software.amazon.awssdk.services.bedrock.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.bedrock.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.bedrock.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrock.model.ThrottlingException;
import software.amazon.awssdk.services.bedrock.model.ValidationException;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestCfnDataProvider.CFN_TAG_LIST_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestCfnDataProvider.INFERENCE_PROFILE_RESOURCE_MODEL;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_STACK_TAG_LIST_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.constructGetInferenceProfileResponse;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<BedrockClient> proxyClient;

    @Mock
    BedrockClient bedrockClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        bedrockClient = mock(BedrockClient.class);
        proxyClient = MOCK_PROXY(proxy, bedrockClient);
    }

    @AfterEach
    public void tear_down() {
        verify(bedrockClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(bedrockClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        // Set up
        final GetInferenceProfileResponse getInferenceProfileResponse = constructGetInferenceProfileResponse(InferenceProfileStatus.ACTIVE);
        when(proxyClient.client().getInferenceProfile(any(GetInferenceProfileRequest.class)))
                .thenReturn(getInferenceProfileResponse);
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(SDK_STACK_TAG_LIST_1).build());
        final ResourceModel expectedResourceModel = Translator.translateFromReadResponse(getInferenceProfileResponse);
        expectedResourceModel.setTags(CFN_TAG_LIST_1);

        final ReadHandler handler = new ReadHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        final ResourceModel responseModel = response.getResourceModel();
        assertThat(responseModel).isEqualTo(expectedResourceModel);

        verify(bedrockClient).getInferenceProfile(any(GetInferenceProfileRequest.class));
        verify(bedrockClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @ParameterizedTest
    @MethodSource("provideExceptionsAndExpectedErrorCode")
    public void handleRequest_throwsException_convertsToCfnException(final Class<Exception> exceptionClass, final HandlerErrorCode expectedErrorCode) {
        // READ action is supposed to return FAILED OperationStatus when ThrottlingException is thrown
        // https://tiny.amazon.com/inoqgbl8/AWSCloudFormationRPDKJavaPlugin/mainline/AmazonWebServicesClientProxy.java#L374-L377
        proxy.setAction(Action.READ);
        when(proxyClient.client().getInferenceProfile(any(GetInferenceProfileRequest.class)))
                .thenThrow(exceptionClass);

        final ReadHandler handler = new ReadHandler();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(expectedErrorCode);

        verify(bedrockClient).getInferenceProfile(any(GetInferenceProfileRequest.class));
    }

    // https://tiny.amazon.com/6elc5r1e/AmazonBedrockControlPlaneServiceModel/mainline/GetInferenceProfile.smithy#L11-L15
    public static Stream<Arguments> provideExceptionsAndExpectedErrorCode() {
        return Stream.of(
                Arguments.of(AccessDeniedException.class, HandlerErrorCode.AccessDenied),
                Arguments.of(ValidationException.class, HandlerErrorCode.InvalidRequest),
                Arguments.of(ResourceNotFoundException.class, HandlerErrorCode.NotFound),
                Arguments.of(ThrottlingException.class, HandlerErrorCode.Throttling),
                Arguments.of(InternalServerException.class, HandlerErrorCode.ServiceInternalError)
        );
    }
}
