package software.amazon.bedrock.applicationinferenceprofile;

import software.amazon.awssdk.services.bedrock.BedrockClient;

import software.amazon.awssdk.services.bedrock.model.DeleteInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.DeleteInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<BedrockClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        logger.log(String.format("Handling DeleteHandler request for AccountId %s", request.getAwsAccountId()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        deleteInferenceProfile(proxy, proxyClient, progress))
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteInferenceProfile(final AmazonWebServicesClientProxy proxy,
                                                                                 final ProxyClient<BedrockClient> proxyClient,
                                                                                 final ProgressEvent<ResourceModel, CallbackContext> progress) {
        return proxy.initiate("AWS-Bedrock-ApplicationInferenceProfile::Delete", proxyClient,
                        progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((deleteModelGatewayRequest, client) ->
                        client.injectCredentialsAndInvokeV2(deleteModelGatewayRequest, client.client()::deleteInferenceProfile))
                .stabilize(this::isStabilized)
                .handleError(BaseHandlerStd::handleError)
                .progress();
    }

    private boolean isStabilized(final DeleteInferenceProfileRequest deleteInferenceProfileRequest,
                                 final DeleteInferenceProfileResponse deleteInferenceProfileResponse,
                                 final ProxyClient<BedrockClient> client,
                                 final ResourceModel model,
                                 final CallbackContext callbackContext) {
        final GetInferenceProfileRequest getInferenceProfileRequest = GetInferenceProfileRequest.builder()
                .inferenceProfileIdentifier(deleteInferenceProfileRequest.inferenceProfileIdentifier())
                .build();

        final GetInferenceProfileResponse getInferenceProfileResponse;
        try {
            getInferenceProfileResponse = client.injectCredentialsAndInvokeV2(getInferenceProfileRequest,
                    client.client()::getInferenceProfile);
        } catch (final ResourceNotFoundException e) {
            return true;
        }
        return false;
    }
}
