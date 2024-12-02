package software.amazon.bedrock.applicationinferenceprofile;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrock.model.ConflictException;
import software.amazon.awssdk.services.bedrock.model.CreateInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileStatus;
import software.amazon.awssdk.services.bedrock.model.InternalServerException;
import software.amazon.awssdk.services.bedrock.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.bedrock.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.bedrock.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrock.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.bedrock.model.ThrottlingException;
import software.amazon.awssdk.services.bedrock.model.TooManyTagsException;
import software.amazon.awssdk.services.bedrock.model.ValidationException;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestCfnDataProvider.CFN_RESOURCE_MODEL_FOR_CREATE_REQUEST;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.CLIENT_REQUEST_TOKEN;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestCfnDataProvider.INFERENCE_PROFILE_RESOURCE_MODEL;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.CREATE_INFERENCE_PROFILE_RESPONSE;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_STACK_TAG_LIST_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.constructGetInferenceProfileResponse;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

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
        when(proxyClient.client().createInferenceProfile(any(CreateInferenceProfileRequest.class)))
                .thenReturn(CREATE_INFERENCE_PROFILE_RESPONSE);
        when(proxyClient.client().getInferenceProfile(any(GetInferenceProfileRequest.class)))
                .thenReturn(constructGetInferenceProfileResponse(InferenceProfileStatus.ACTIVE))
                .thenReturn(constructGetInferenceProfileResponse(InferenceProfileStatus.ACTIVE));
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(SDK_STACK_TAG_LIST_1).build());

        final CreateHandler handler = new CreateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceState(CFN_RESOURCE_MODEL_FOR_CREATE_REQUEST)
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getInferenceProfileArn()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        final ResourceModel resourceModel = response.getResourceModel();
        assertThat(resourceModel.getInferenceProfileArn()).isEqualTo(INFERENCE_PROFILE_RESOURCE_MODEL.getInferenceProfileArn());
        assertThat(resourceModel.getInferenceProfileIdentifier()).isEqualTo(INFERENCE_PROFILE_RESOURCE_MODEL.getInferenceProfileArn());
        assertThat(resourceModel.getTags().size()).isEqualTo(INFERENCE_PROFILE_RESOURCE_MODEL.getTags().size());

        verify(bedrockClient).createInferenceProfile(any(CreateInferenceProfileRequest.class));
        verify(bedrockClient, times(2)).getInferenceProfile(any(GetInferenceProfileRequest.class));
    }

    @ParameterizedTest
    @MethodSource("provideExceptionsAndExpectedResult")
    public void handleRequest_throwsException_convertsToCfnException(final Class<Exception> exceptionClass,
                                                                     final OperationStatus expectedStatus,
                                                                     final HandlerErrorCode expectedErrorCode) {
        // Set up
        when(proxyClient.client().getInferenceProfile(any(GetInferenceProfileRequest.class)))
                .thenThrow(exceptionClass);

        final ReadHandler handler = new ReadHandler();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        assertThat(response.getErrorCode()).isEqualTo(expectedErrorCode);

        verify(bedrockClient).getInferenceProfile(any(GetInferenceProfileRequest.class));
    }

    // https://tiny.amazon.com/1hutrtutq/AmazonBedrockControlPlaneServiceModel/f5ef3fb6/CreateInferenceProfile.smithy#L12-L19
    public static Stream<Arguments> provideExceptionsAndExpectedResult() {
        return Stream.of(
                Arguments.of(AccessDeniedException.class, OperationStatus.FAILED, HandlerErrorCode.AccessDenied),
                Arguments.of(InternalServerException.class, OperationStatus.FAILED, HandlerErrorCode.ServiceInternalError),
                // CfnThrottlingException is retriable
                Arguments.of(ThrottlingException.class, OperationStatus.IN_PROGRESS, HandlerErrorCode.Throttling),
                Arguments.of(ValidationException.class, OperationStatus.FAILED, HandlerErrorCode.InvalidRequest),
                Arguments.of(ResourceNotFoundException.class, OperationStatus.FAILED, HandlerErrorCode.NotFound),
                Arguments.of(ConflictException.class, OperationStatus.FAILED, HandlerErrorCode.ResourceConflict),
                Arguments.of(ServiceQuotaExceededException.class, OperationStatus.FAILED, HandlerErrorCode.ServiceLimitExceeded),
                Arguments.of(TooManyTagsException.class, OperationStatus.FAILED, HandlerErrorCode.InvalidRequest)
        );
    }
}
