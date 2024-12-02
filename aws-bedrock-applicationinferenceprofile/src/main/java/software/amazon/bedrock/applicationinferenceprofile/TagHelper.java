package software.amazon.bedrock.applicationinferenceprofile;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class TagHelper {
    /**
     * convertToMap
     * <p>
     * Converts a collection of Tag objects to a tag-name -> tag-value map.
     * <p>
     * Note: Tag objects with null tag values will not be included in the output
     * map.
     *
     * @param tags Collection of tags to convert
     * @return Converted Map of tags
     */
    public static Map<String, String> convertToMap(final Collection<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyMap();
        }
        return tags.stream()
                .filter(tag -> tag.value() != null)
                .collect(Collectors.toMap(
                        Tag::key,
                        Tag::value,
                        (oldValue, newValue) -> newValue));
    }

    /**
     * convertToSet
     * <p>
     * Converts a tag map to a set of Tag objects.
     * <p>
     * Note: Like convertToMap, convertToSet filters out value-less tag entries.
     *
     * @param tagMap Map of tags to convert
     * @return Set of Tag objects
     */
    public static Set<Tag> convertToSet(final Map<String, String> tagMap) {
        if (MapUtils.isEmpty(tagMap)) {
            return Collections.emptySet();
        }
        return tagMap.entrySet().stream()
                .filter(tag -> tag.getValue() != null)
                .map(tag -> Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toSet());
    }

    public static List<Tag> convertToList(final Map<String, String> tagMap) {
        if (MapUtils.isEmpty(tagMap)) {
            return Collections.emptyList();
        }
        return tagMap.entrySet().stream()
                .filter(tag -> tag.getValue() != null)
                .map(tag -> Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * shouldUpdateTags
     * <p>
     * Determines whether user defined tags have been changed during update.
     */
    public final boolean shouldUpdateTags(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> previousTags = getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = getNewDesiredTags(handlerRequest);
        return ObjectUtils.notEqual(previousTags, desiredTags);
    }

    /**
     * getPreviouslyAttachedTags
     * <p>
     * If stack tags and resource tags are not merged together in Configuration class,
     * we will get previously attached system (with `aws:cloudformation` prefix) and user defined tags from
     * handlerRequest.getPreviousSystemTags() (system tags),
     * handlerRequest.getPreviousResourceTags() (stack tags),
     * handlerRequest.getPreviousResourceState().getTags() (resource tags).
     * <p>
     * System tags are an optional feature. Merge them to your tags if you have enabled them for your resource.
     * System tags can change on resource update if the resource is imported to the stack.
     */
    public static Map<String, String> getPreviouslyAttachedTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> previousTags = new HashMap<>();
        // get previous system tags
        if (handlerRequest.getPreviousSystemTags() != null) {
            previousTags.putAll(handlerRequest.getPreviousSystemTags());
        }

        // get previous stack level tags from handlerRequest
        if (handlerRequest.getPreviousResourceTags() != null) {
            previousTags.putAll(handlerRequest.getPreviousResourceTags());
        }

        // get resource level tags from previous resource state based on your tag property name
        if (handlerRequest.getPreviousResourceState() != null && handlerRequest.getPreviousResourceState().getTags() != null) {
            previousTags.putAll(convertToMap(Translator.translateToSdkTags(handlerRequest.getPreviousResourceState().getTags())));
        }
        return previousTags;
    }

    /**
     * getNewDesiredTags
     * <p>
     * If stack tags and resource tags are not merged together in Configuration class,
     * we will get new desired system (with `aws:cloudformation` prefix) and user defined tags from
     * handlerRequest.getSystemTags() (system tags),
     * handlerRequest.getDesiredResourceTags() (stack tags),
     * handlerRequest.getDesiredResourceState().getTags() (resource tags).
     * <p>
     * System tags are an optional feature. Merge them to your tags if you have enabled them for your resource.
     * System tags can change on resource update if the resource is imported to the stack.
     */
    public static Map<String, String> getNewDesiredTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> desiredTags = new HashMap<>();

        if (handlerRequest.getSystemTags() != null) {
            desiredTags.putAll(handlerRequest.getSystemTags());
        }

        // get desired stack level tags from handlerRequest
        if (handlerRequest.getDesiredResourceTags() != null) {
            desiredTags.putAll(handlerRequest.getDesiredResourceTags());
        }

        // get resource level tags from resource model based on your tag property name
        if (handlerRequest.getDesiredResourceState() != null && handlerRequest.getDesiredResourceState().getTags() != null) {
            desiredTags.putAll(convertToMap(Translator.translateToSdkTags(handlerRequest.getDesiredResourceState().getTags())));
        }
        return desiredTags;
    }

    /**
     * generateTagsToAdd
     * <p>
     * Determines the tags the customer desired to define or redefine.
     */
    public static Map<String, String> generateTagsToAdd(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
        return desiredTags.entrySet().stream()
                .filter(e -> !previousTags.containsKey(e.getKey()) || !Objects.equals(previousTags.get(e.getKey()), e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }

    /**
     * getTagsToRemove
     * <p>
     * Determines the tags the customer desired to remove from the function.
     */
    public static Set<String> generateTagsToRemove(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
        final Set<String> desiredTagNames = desiredTags.keySet();

        return previousTags.keySet().stream()
                .filter(tagName -> !desiredTagNames.contains(tagName))
                .collect(Collectors.toSet());
    }

    /**
     * generateTagsToAdd
     * <p>
     * Determines the tags the customer desired to define or redefine.
     */
    public Set<Tag> generateTagsToAdd(final Set<Tag> previousTags, final Set<Tag> desiredTags) {
        return Sets.difference(new HashSet<>(desiredTags), new HashSet<>(previousTags));
    }

    /**
     * getTagsToRemove
     * <p>
     * Determines the tags the customer desired to remove from the function.
     */
    public Set<Tag> generateTagsToRemove(final Set<Tag> previousTags, final Set<Tag> desiredTags) {
        return Sets.difference(new HashSet<>(previousTags), new HashSet<>(desiredTags));
    }


    /**
     * tagResource during update
     * <p>
     * Calls the service:TagResource API.
     */
    public static ProgressEvent<ResourceModel, CallbackContext>
    tagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<BedrockClient> serviceClient, final ResourceModel resourceModel,
                final ResourceHandlerRequest<ResourceModel> handlerRequest, final CallbackContext callbackContext, final Map<String, String> addedTags, final Logger logger) {
        logger.log(String.format("[UPDATE][IN PROGRESS] Going to add tags for ApplicationInferenceProfile resource: " +
                        "%s with AccountId: %s",
                resourceModel.getInferenceProfileIdentifier(), handlerRequest.getAwsAccountId()));

        return proxy.initiate("AWS-Bedrock-ApplicationInferenceProfile::TagOps", serviceClient, resourceModel, callbackContext)
                .translateToServiceRequest(model ->
                        Translator.tagResourceRequest(model, addedTags))
                .makeServiceCall((request, client) ->
                        proxy.injectCredentialsAndInvokeV2(request, client.client()::tagResource))
                .handleError(BaseHandlerStd::handleError)
                .progress();
    }

    /**
     * untagResource during update
     * <p>
     * Calls the service:UntagResource API.
     */
    public static ProgressEvent<ResourceModel, CallbackContext>
    untagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<BedrockClient> serviceClient, final ResourceModel resourceModel,
                  final ResourceHandlerRequest<ResourceModel> handlerRequest, final CallbackContext callbackContext, final Set<String> removedTags, final Logger logger) {
        logger.log(String.format("[UPDATE][IN PROGRESS] Going to remove tags for ApplicationInferenceProfile resource: %s with AccountId: %s",
                resourceModel.getInferenceProfileIdentifier(), handlerRequest.getAwsAccountId()));

        return proxy.initiate("AWS-Bedrock-ApplicationInferenceProfile::TagOps", serviceClient, resourceModel, callbackContext)
                .translateToServiceRequest(model ->
                        Translator.untagResourceRequest(model, removedTags))
                .makeServiceCall((request, client) ->
                        proxy.injectCredentialsAndInvokeV2(request, client.client()::untagResource))
                .handleError(BaseHandlerStd::handleError)
                .progress();
    }
}
