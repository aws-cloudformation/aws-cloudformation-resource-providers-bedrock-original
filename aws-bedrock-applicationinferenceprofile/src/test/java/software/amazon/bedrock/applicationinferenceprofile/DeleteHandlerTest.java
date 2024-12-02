package software.amazon.bedrock.applicationinferenceprofile;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrock.model.ConflictException;
import software.amazon.awssdk.services.bedrock.model.DeleteInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.DeleteInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileStatus;
import software.amazon.awssdk.services.bedrock.model.InternalServerException;
import software.amazon.awssdk.services.bedrock.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrock.model.ThrottlingException;
import software.amazon.awssdk.services.bedrock.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static software.amazon.bedrock.applicationinferenceprofile.data.TestCfnDataProvider.INFERENCE_PROFILE_RESOURCE_MODEL;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

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
        when(proxyClient.client().deleteInferenceProfile(any(DeleteInferenceProfileRequest.class)))
                .thenReturn(DeleteInferenceProfileResponse.builder().build());
        when(proxyClient.client().getInferenceProfile(any(GetInferenceProfileRequest.class)))
                .thenReturn(GetInferenceProfileResponse.builder().status(InferenceProfileStatus.ACTIVE).build())
                .thenThrow(ResourceNotFoundException.class);

        final DeleteHandler handler = new DeleteHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @ParameterizedTest
    @MethodSource("provideExceptionsAndExpectedResult")
    public void handleRequest_throwsException_convertsToCfnException(final Class<Exception> exceptionClass,
                                                                     final OperationStatus expectedStatus,
                                                                     final HandlerErrorCode expectedErrorCode) {
        final DeleteHandler handler = new DeleteHandler();
        when(proxyClient.client().deleteInferenceProfile(any(DeleteInferenceProfileRequest.class)))
                .thenThrow(exceptionClass);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        assertThat(response.getErrorCode()).isEqualTo(expectedErrorCode);

        verify(bedrockClient).deleteInferenceProfile(any(DeleteInferenceProfileRequest.class));
    }

    // https://tiny.amazon.com/9t7t4bc3/AmazonBedrockControlPlaneServiceModel/mainline/DeleteInferenceProfile.smithy#L11-L16
    public static Stream<Arguments> provideExceptionsAndExpectedResult() {
        return Stream.of(
                Arguments.of(AccessDeniedException.class, OperationStatus.FAILED, HandlerErrorCode.AccessDenied),
                Arguments.of(InternalServerException.class, OperationStatus.FAILED, HandlerErrorCode.ServiceInternalError),
                Arguments.of(ThrottlingException.class, OperationStatus.IN_PROGRESS, HandlerErrorCode.Throttling),
                Arguments.of(ValidationException.class, OperationStatus.FAILED, HandlerErrorCode.InvalidRequest),
                Arguments.of(ResourceNotFoundException.class, OperationStatus.FAILED, HandlerErrorCode.NotFound),
                Arguments.of(ConflictException.class, OperationStatus.FAILED, HandlerErrorCode.ResourceConflict)
        );
    }
}
