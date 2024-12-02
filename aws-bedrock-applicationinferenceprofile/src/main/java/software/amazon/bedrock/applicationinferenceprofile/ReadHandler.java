package software.amazon.bedrock.applicationinferenceprofile;

import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<BedrockClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        logger.log(String.format("Handling ReadHandler request for AccountId %s", request.getAwsAccountId()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progressEvent ->
                        getInferenceProfile(
                                proxy,
                                proxyClient,
                                progressEvent.getResourceModel(),
                                progressEvent.getCallbackContext()))
                .then(progressEvent ->
                        listTagsForInferenceProfile(
                                proxy,
                                proxyClient,
                                progressEvent.getResourceModel(),
                                progressEvent.getCallbackContext()))
                .then(progressEvent ->
                        ProgressEvent.defaultSuccessHandler(progressEvent.getResourceModel()));
    }

    private ProgressEvent<ResourceModel, CallbackContext> getInferenceProfile(final AmazonWebServicesClientProxy proxy,
                                                                              final ProxyClient<BedrockClient> proxyClient,
                                                                              final ResourceModel resourceModel,
                                                                              final CallbackContext callbackContext) {
        return proxy.initiate("AWS-Bedrock-ApplicationInferenceProfile::Read", proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(
                        (getInferenceProfileRequest, client) ->
                                client.injectCredentialsAndInvokeV2(getInferenceProfileRequest, client.client()::getInferenceProfile))
                .handleError(BaseHandlerStd::handleError)
                .done(getInferenceProfileResponse -> ProgressEvent.progress(Translator.translateFromReadResponse(getInferenceProfileResponse), callbackContext));
    }

    private ProgressEvent<ResourceModel, CallbackContext> listTagsForInferenceProfile(final AmazonWebServicesClientProxy proxy,
                                                                                      final ProxyClient<BedrockClient> proxyClient,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext callbackContext) {
        return proxy.initiate("AWS-Bedrock-ApplicationInferenceProfile::ListTags", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToListTagsRequest)
                .makeServiceCall((listTagsRequest, client) -> client.injectCredentialsAndInvokeV2(listTagsRequest,
                        client.client()::listTagsForResource))
                .handleError(BaseHandlerStd::handleError)
                .done((listTagsRequest, listTagsResponse, client, resourceModel, resourceCallbackContext) -> {
                    resourceModel.setTags(Translator.translateFromSdkTags(listTagsResponse.tags()));
                    return ProgressEvent.progress(resourceModel, resourceCallbackContext);
                });
    }
}
