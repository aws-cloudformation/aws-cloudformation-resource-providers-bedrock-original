package software.amazon.bedrock.applicationinferenceprofile;

import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.services.bedrock.BedrockClient;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<BedrockClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        logger.log(String.format("Handling UpdateHandler request for AccountId %s", request.getAwsAccountId()));

        final Map<String, String> desiredTags = TagHelper.getNewDesiredTags(request);
        final Map<String, String> previousTags = TagHelper.getPreviouslyAttachedTags(request);
        final Map<String, String> tagsToAdd = TagHelper.generateTagsToAdd(previousTags, desiredTags);
        final Set<String> tagKeysToRemove = TagHelper.generateTagsToRemove(previousTags, desiredTags);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(
                        progress -> tagsToAdd.isEmpty() ?
                                progress :
                                TagHelper.tagResource(
                                        proxy,
                                        proxyClient,
                                        progress.getResourceModel(),
                                        request,
                                        progress.getCallbackContext(),
                                        tagsToAdd,
                                        logger))
                .then(
                        progress -> tagKeysToRemove.isEmpty() ?
                                progress :
                                TagHelper.untagResource(
                                        proxy,
                                        proxyClient,
                                        progress.getResourceModel(),
                                        request,
                                        progress.getCallbackContext(),
                                        tagKeysToRemove,
                                        logger))
                .then(
                        progress -> new ReadHandler().handleRequest(
                                proxy,
                                request,
                                callbackContext,
                                proxyClient,
                                logger));
    }
}
