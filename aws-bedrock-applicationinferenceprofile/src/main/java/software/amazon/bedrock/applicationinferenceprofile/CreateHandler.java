package software.amazon.bedrock.applicationinferenceprofile;

import java.util.List;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.CreateInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.CreateInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileResponse;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<BedrockClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        logger.log(String.format("Handling CreateHandler request for AccountId %s and clientRequestToken %s",
                request.getAwsAccountId(), request.getClientRequestToken()));

        /**
         * 1. Put all system tags, stack level tags and resource level tags into a HashMap (all of them are SDK Tags)
         * 2. Convert the HashMap to a list
         * 3. Translate to CFN Tags
         */
        final List<Tag> tags = Translator.translateFromSdkTags(TagHelper.convertToList(TagHelper.getNewDesiredTags(request)));
        request.getDesiredResourceState().setTags(tags);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progressEvent ->
                        createInferenceProfile(
                                proxy,
                                proxyClient,
                                progressEvent,
                                request.getClientRequestToken()))
                .then(progressEvent ->
                        new ReadHandler().handleRequest(
                                proxy,
                                request,
                                progressEvent.getCallbackContext(),
                                proxyClient,
                                logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInferenceProfile(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<BedrockClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final String clientRequestToken) {
        return proxy.initiate("AWS-Bedrock-ApplicationInferenceProfile::Create", proxyClient,
                        progressEvent.getResourceModel(), progressEvent.getCallbackContext())
                .translateToServiceRequest(model -> Translator.translateToCreateRequest(progressEvent.getResourceModel(), clientRequestToken))
                .makeServiceCall((createInferenceProfileRequest, client) ->
                        client.injectCredentialsAndInvokeV2(createInferenceProfileRequest, client.client()::createInferenceProfile))
                .stabilize(this::isStabilized)
                .handleError(BaseHandlerStd::handleError)
                .progress();
    }

    private boolean isStabilized(final CreateInferenceProfileRequest createInferenceProfileRequest,
                                 final CreateInferenceProfileResponse createInferenceProfileResponse,
                                 final ProxyClient<BedrockClient> client,
                                 final ResourceModel model,
                                 final CallbackContext callbackContext) {
        final GetInferenceProfileRequest getInferenceProfileRequest = GetInferenceProfileRequest.builder()
                .inferenceProfileIdentifier(createInferenceProfileResponse.inferenceProfileArn())
                .build();
        final GetInferenceProfileResponse getInferenceProfileResponse =
                client.injectCredentialsAndInvokeV2(getInferenceProfileRequest, client.client()::getInferenceProfile);

        switch (getInferenceProfileResponse.status()) {
        case ACTIVE:
            logger.log(String.format("%s [%s] has stabilized.", ResourceModel.TYPE_NAME,
                    getInferenceProfileResponse.inferenceProfileArn()));
            model.setInferenceProfileIdentifier(createInferenceProfileResponse.inferenceProfileArn());
            model.setInferenceProfileArn(createInferenceProfileResponse.inferenceProfileArn());
            return true;
        default:
            final String errorMessage = String.format("%s [%s] failed to create.",
                    ResourceModel.TYPE_NAME, createInferenceProfileRequest.inferenceProfileName());
            throw new CfnServiceInternalErrorException(new Throwable(errorMessage));
        }
    }
}
