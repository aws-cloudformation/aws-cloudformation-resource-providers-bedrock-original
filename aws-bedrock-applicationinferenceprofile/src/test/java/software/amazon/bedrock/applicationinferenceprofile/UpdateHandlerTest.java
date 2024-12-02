package software.amazon.bedrock.applicationinferenceprofile;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrock.model.ConflictException;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileStatus;
import software.amazon.awssdk.services.bedrock.model.InternalServerException;
import software.amazon.awssdk.services.bedrock.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.bedrock.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.bedrock.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrock.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.bedrock.model.TagResourceRequest;
import software.amazon.awssdk.services.bedrock.model.TagResourceResponse;
import software.amazon.awssdk.services.bedrock.model.ThrottlingException;
import software.amazon.awssdk.services.bedrock.model.TooManyTagsException;
import software.amazon.awssdk.services.bedrock.model.UntagResourceRequest;
import software.amazon.awssdk.services.bedrock.model.UntagResourceResponse;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestCfnDataProvider.INFERENCE_PROFILE_RESOURCE_MODEL;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestCfnDataProvider.INFERENCE_PROFILE_RESOURCE_MODEL_WITHOUT_RESOURCE_TAGS;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_INFERENCE_PROFILE_MODEL_LIST;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_RESOURCE_TAG_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_RESOURCE_TAG_2;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_STACK_TAG_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_STACK_TAG_2;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.constructGetInferenceProfileResponse;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

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
    public void handleRequest_AddStackTagSuccess() {
        // Set up
        final GetInferenceProfileResponse inferenceProfile =
                constructGetInferenceProfileResponse(InferenceProfileStatus.ACTIVE).toBuilder()
                        .models(SDK_INFERENCE_PROFILE_MODEL_LIST)
                        .build();
        when(proxyClient.client().tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());
        when(proxyClient.client().getInferenceProfile(any(GetInferenceProfileRequest.class)))
                .thenReturn(inferenceProfile);

        final List<software.amazon.awssdk.services.bedrock.model.Tag> expectedTagList =
                List.of(SDK_STACK_TAG_1, SDK_STACK_TAG_1, SDK_RESOURCE_TAG_1, SDK_RESOURCE_TAG_2);
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(expectedTagList)
                        .build());

        final ResourceModel expectedResourceModel = Translator.translateFromReadResponse(inferenceProfile);
        expectedResourceModel.setTags(Translator.translateFromSdkTags(expectedTagList));

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .desiredResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .previousResourceTags(TagHelper.convertToMap(List.of(SDK_STACK_TAG_1)))
                .desiredResourceTags(TagHelper.convertToMap(List.of(SDK_STACK_TAG_1, SDK_STACK_TAG_2)))
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(bedrockClient).tagResource(any(TagResourceRequest.class));
        verify(bedrockClient).getInferenceProfile(any(GetInferenceProfileRequest.class));
        verify(bedrockClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_RemoveStackTagSuccess() {
        // Set up
        final GetInferenceProfileResponse inferenceProfile =
                constructGetInferenceProfileResponse(InferenceProfileStatus.ACTIVE).toBuilder()
                        .models(SDK_INFERENCE_PROFILE_MODEL_LIST)
                        .build();
        when(proxyClient.client().untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());
        when(proxyClient.client().getInferenceProfile(any(GetInferenceProfileRequest.class)))
                .thenReturn(inferenceProfile);

        final List<software.amazon.awssdk.services.bedrock.model.Tag> expectedTagList =
                List.of(SDK_STACK_TAG_1, SDK_RESOURCE_TAG_1, SDK_RESOURCE_TAG_2);
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(expectedTagList).build());

        final ResourceModel expectedResourceModel = Translator.translateFromReadResponse(inferenceProfile);
        expectedResourceModel.setTags(Translator.translateFromSdkTags(expectedTagList));

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .desiredResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .previousResourceTags(TagHelper.convertToMap(List.of(SDK_STACK_TAG_1, SDK_STACK_TAG_2)))
                .desiredResourceTags(TagHelper.convertToMap(List.of(SDK_STACK_TAG_1)))
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(bedrockClient).untagResource(any(UntagResourceRequest.class));
        verify(bedrockClient).getInferenceProfile(any(GetInferenceProfileRequest.class));
        verify(bedrockClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_AddResourceTagSuccess() {
        // Set up
        final GetInferenceProfileResponse inferenceProfile =
                constructGetInferenceProfileResponse(InferenceProfileStatus.ACTIVE).toBuilder()
                        .models(SDK_INFERENCE_PROFILE_MODEL_LIST)
                        .build();
        when(proxyClient.client().tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());
        when(proxyClient.client().getInferenceProfile(any(GetInferenceProfileRequest.class)))
                .thenReturn(inferenceProfile);

        final List<software.amazon.awssdk.services.bedrock.model.Tag> expectedTagList =
                List.of(SDK_STACK_TAG_1, SDK_STACK_TAG_1, SDK_RESOURCE_TAG_1, SDK_RESOURCE_TAG_2);
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(expectedTagList)
                        .build());

        final ResourceModel expectedResourceModel = Translator.translateFromReadResponse(inferenceProfile);
        expectedResourceModel.setTags(Translator.translateFromSdkTags(expectedTagList));

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(INFERENCE_PROFILE_RESOURCE_MODEL_WITHOUT_RESOURCE_TAGS)
                .desiredResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .previousResourceTags(TagHelper.convertToMap(List.of(SDK_STACK_TAG_1, SDK_STACK_TAG_2)))
                .desiredResourceTags(TagHelper.convertToMap(List.of(SDK_STACK_TAG_1, SDK_STACK_TAG_2)))
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(bedrockClient).tagResource(any(TagResourceRequest.class));
        verify(bedrockClient).getInferenceProfile(any(GetInferenceProfileRequest.class));
        verify(bedrockClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_RemoveResourceTagSuccess() {
        // Set up
        final GetInferenceProfileResponse inferenceProfile =
                constructGetInferenceProfileResponse(InferenceProfileStatus.ACTIVE).toBuilder()
                        .models(SDK_INFERENCE_PROFILE_MODEL_LIST)
                        .build();
        when(proxyClient.client().untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());
        when(proxyClient.client().getInferenceProfile(any(GetInferenceProfileRequest.class)))
                .thenReturn(inferenceProfile);

        final List<software.amazon.awssdk.services.bedrock.model.Tag> expectedTagList =
                List.of(SDK_STACK_TAG_1, SDK_STACK_TAG_1, SDK_RESOURCE_TAG_1, SDK_RESOURCE_TAG_2);
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(expectedTagList)
                        .build());

        final ResourceModel expectedResourceModel = Translator.translateFromReadResponse(inferenceProfile);
        expectedResourceModel.setTags(Translator.translateFromSdkTags(expectedTagList));

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .desiredResourceState(INFERENCE_PROFILE_RESOURCE_MODEL_WITHOUT_RESOURCE_TAGS)
                .previousResourceTags(TagHelper.convertToMap(List.of(SDK_STACK_TAG_1, SDK_STACK_TAG_2)))
                .desiredResourceTags(TagHelper.convertToMap(List.of(SDK_STACK_TAG_1, SDK_STACK_TAG_2)))
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(bedrockClient).untagResource(any(UntagResourceRequest.class));
        verify(bedrockClient).getInferenceProfile(any(GetInferenceProfileRequest.class));
        verify(bedrockClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }


    @ParameterizedTest
    @MethodSource("provideExceptionsAndExpectedResult")
    public void tagResource_throwsException_convertsToCfnException(final Class<Exception> exceptionClass,
                                                                     final OperationStatus expectedStatus,
                                                                     final HandlerErrorCode expectedErrorCode) {
        // Set up
        when(proxyClient.client().tagResource(any(TagResourceRequest.class)))
                .thenThrow(exceptionClass);

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(INFERENCE_PROFILE_RESOURCE_MODEL_WITHOUT_RESOURCE_TAGS)
                .desiredResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        assertThat(response.getErrorCode()).isEqualTo(expectedErrorCode);

        verify(bedrockClient).tagResource(any(TagResourceRequest.class));
    }

    @ParameterizedTest
    @MethodSource("provideExceptionsAndExpectedResult")
    public void untagResource_throwsException_convertsToCfnException(final Class<Exception> exceptionClass,
                                                                     final OperationStatus expectedStatus,
                                                                     final HandlerErrorCode expectedErrorCode) {
        // Set up
        when(proxyClient.client().untagResource(any(UntagResourceRequest.class)))
                .thenThrow(exceptionClass);

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(INFERENCE_PROFILE_RESOURCE_MODEL)
                .desiredResourceState(INFERENCE_PROFILE_RESOURCE_MODEL_WITHOUT_RESOURCE_TAGS)
                .build();

        // Trigger
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        assertThat(response.getErrorCode()).isEqualTo(expectedErrorCode);

        verify(bedrockClient).untagResource(any(UntagResourceRequest.class));
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
                Arguments.of(TooManyTagsException.class, OperationStatus.FAILED, HandlerErrorCode.InvalidRequest)
        );
    }
}
