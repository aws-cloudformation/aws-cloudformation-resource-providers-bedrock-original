package software.amazon.bedrock.applicationinferenceprofile;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrock.model.ConflictException;
import software.amazon.awssdk.services.bedrock.model.InternalServerException;
import software.amazon.awssdk.services.bedrock.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrock.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.bedrock.model.ThrottlingException;
import software.amazon.awssdk.services.bedrock.model.TooManyTagsException;
import software.amazon.awssdk.services.bedrock.model.ValidationException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        return handleRequest(
            proxy,
            request,
            callbackContext != null ? callbackContext : new CallbackContext(),
            proxy.newProxy(ClientBuilder::getClient),
            logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<BedrockClient> proxyClient,
        final Logger logger);

    public static ProgressEvent<ResourceModel, CallbackContext> handleError(
        final AwsRequest request,
        final Exception e,
        final ProxyClient<BedrockClient> proxyClient,
        final ResourceModel resourceModel,
        final CallbackContext callbackContext)
        throws Exception {

        final BaseHandlerException ex;

        if (e instanceof ServiceQuotaExceededException) {
            ex = new CfnServiceLimitExceededException(e);
        } else if (e instanceof AccessDeniedException) {
            ex = new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } else if (e instanceof ValidationException) {
            ex = new CfnInvalidRequestException(e);
        } else if (e instanceof ConflictException) {
            ex = new CfnResourceConflictException(e);
        } else if (e instanceof ResourceNotFoundException) {
            ex = new CfnNotFoundException(e);
        } else if (e instanceof ThrottlingException) {
            ex = new CfnThrottlingException(e);
        } else if (e instanceof InternalServerException) {
            ex = new CfnServiceInternalErrorException(e);
        } else if (e instanceof TooManyTagsException) {
            ex = new CfnInvalidRequestException(e);
        } else {
            ex = new CfnInternalFailureException(e);
        }
        return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
    }
}
