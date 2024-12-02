package software.amazon.bedrock.applicationinferenceprofile;

import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileType;
import software.amazon.awssdk.services.bedrock.model.ListInferenceProfilesRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<BedrockClient> proxyClient,
            final Logger logger) {

        logger.log(String.format("Handling ListHandler request for AccountId %s", request.getAwsAccountId()));

        final int maxResults = request.getMaxResults() == 0 ? 100 : request.getMaxResults();
        final String type = request.getDesiredResourceState().getType() == null
                ? InferenceProfileType.APPLICATION.toString()
                : request.getDesiredResourceState().getType();
        return proxy.initiate("AWS-Bedrock-ApplicationInferenceProfile::List", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(resourceModel -> ListInferenceProfilesRequest.builder()
                        .nextToken(request.getNextToken())
                        .maxResults(maxResults)
                        .typeEquals(type)
                        .build())
                .makeServiceCall((listInferenceProfileRequest, client) ->
                        client.injectCredentialsAndInvokeV2(listInferenceProfileRequest, client.client()::listInferenceProfiles))
                .handleError(BaseHandlerStd::handleError)
                .done(listInferenceProfileResponse ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .resourceModels(Translator.translateFromListResponse(listInferenceProfileResponse))
                                .nextToken(listInferenceProfileResponse.nextToken())
                                .status(OperationStatus.SUCCESS)
                                .build());
    }
}
