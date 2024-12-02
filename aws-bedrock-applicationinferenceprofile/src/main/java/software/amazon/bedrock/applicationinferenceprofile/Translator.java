package software.amazon.bedrock.applicationinferenceprofile;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.services.bedrock.model.CreateInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.DeleteInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileRequest;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileModelSource;
import software.amazon.awssdk.services.bedrock.model.ListInferenceProfilesResponse;
import software.amazon.awssdk.services.bedrock.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.bedrock.model.TagResourceRequest;
import software.amazon.awssdk.services.bedrock.model.UntagResourceRequest;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - translation between AwsJavaSdk-Bedrock models and RPDK (Registry Provider Development Kit) models
 * - resource model construction for read/list handlers
 */

public class Translator {

    /**
     * Request to create a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    public static CreateInferenceProfileRequest translateToCreateRequest(final ResourceModel model, final String clientRequestToken) {
        return CreateInferenceProfileRequest.builder()
                .clientRequestToken(clientRequestToken)
                .tags(translateToSdkTags(model.getTags()))
                .modelSource(InferenceProfileModelSource.builder().copyFrom(model.getModelSource().getCopyFrom()).build())
                .inferenceProfileName(model.getInferenceProfileName())
                .description(model.getDescription())
                .build();
    }


    static GetInferenceProfileRequest translateToReadRequest(final ResourceModel model) {
        return GetInferenceProfileRequest.builder()
                .inferenceProfileIdentifier(model.getInferenceProfileIdentifier())
                .build();
    }

    static ResourceModel translateFromReadResponse(final GetInferenceProfileResponse response) {
        return ResourceModel.builder()
                .inferenceProfileIdentifier(response.inferenceProfileArn())
                .inferenceProfileName(response.inferenceProfileName())
                .inferenceProfileArn(response.inferenceProfileArn())
                .inferenceProfileId(response.inferenceProfileId())
                .description(response.description())
                .status(response.statusAsString())
                .type(response.typeAsString())
                .createdAt(toStringIfNotNull(response.createdAt()))
                .updatedAt(toStringIfNotNull(response.updatedAt()))
                .models(translateFromSdkInferenceProfileModels(response.models()))
                .build();
    }

    public static ListTagsForResourceRequest translateToListTagsRequest(final ResourceModel model) {
        return ListTagsForResourceRequest.builder()
                .resourceARN(model.getInferenceProfileIdentifier())
                .build();
    }

    public static List<software.amazon.awssdk.services.bedrock.model.InferenceProfileModel> translateToSdkInferenceProfileModels(final List<InferenceProfileModel> inferenceProfileModels) {
        return streamOfOrEmpty(inferenceProfileModels)
                .map(inferenceProfileModel -> software.amazon.awssdk.services.bedrock.model.InferenceProfileModel.builder()
                        .modelArn(inferenceProfileModel.getModelArn())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Translate a list of SDK InferenceProfileModel to a list of RPDK/CFN InferenceProfileModel
     *
     * @param inferenceProfileModels SDK InferenceProfileModel
     * @return CFN InferenceProfileModel
     */
    public static List<InferenceProfileModel> translateFromSdkInferenceProfileModels(final List<software.amazon.awssdk.services.bedrock.model.InferenceProfileModel> inferenceProfileModels) {
        return streamOfOrEmpty(inferenceProfileModels)
                .map(inferenceProfileModel -> InferenceProfileModel.builder()
                        .modelArn(inferenceProfileModel.modelArn())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Request to delete a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static DeleteInferenceProfileRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteInferenceProfileRequest.builder()
                .inferenceProfileIdentifier(model.getInferenceProfileIdentifier())
                .build();
    }

    static List<ResourceModel> translateFromListResponse(final ListInferenceProfilesResponse response) {
        return streamOfOrEmpty(response.inferenceProfileSummaries())
                .map(inferenceProfileSummary -> ResourceModel.builder()
                        .inferenceProfileIdentifier(inferenceProfileSummary.inferenceProfileArn())
                        .inferenceProfileArn(inferenceProfileSummary.inferenceProfileArn())
                        .inferenceProfileName(inferenceProfileSummary.inferenceProfileName())
                        .inferenceProfileId(inferenceProfileSummary.inferenceProfileId())
                        .type(inferenceProfileSummary.typeAsString())
                        .status(inferenceProfileSummary.statusAsString())
                        .description(inferenceProfileSummary.description())
                        .models(translateFromSdkInferenceProfileModels(inferenceProfileSummary.models()))
                        .createdAt(toStringIfNotNull(inferenceProfileSummary.createdAt()))
                        .updatedAt(toStringIfNotNull(inferenceProfileSummary.updatedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    /**
     * Request to add tags to a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static TagResourceRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
        return TagResourceRequest.builder()
                .tags(Translator.translateToSdkTags(addedTags))
                .resourceARN(model.getInferenceProfileIdentifier())
                .build();
    }

    /**
     * Request to add tags to a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static UntagResourceRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
        return UntagResourceRequest.builder()
                .resourceARN(model.getInferenceProfileIdentifier())
                .tagKeys(removedTags)
                .build();
    }

    public static List<software.amazon.awssdk.services.bedrock.model.Tag> translateToSdkTags(final List<Tag> tags) {
        return streamOfOrEmpty(tags)
                .map(tag -> software.amazon.awssdk.services.bedrock.model.Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    public static List<software.amazon.awssdk.services.bedrock.model.Tag> translateToSdkTags(final Map<String, String> tags) {
        List<software.amazon.awssdk.services.bedrock.model.Tag> sdkTags = new ArrayList<>();

        for (Map.Entry<String, String> entry : tags.entrySet()) {
            final software.amazon.awssdk.services.bedrock.model.Tag tag =
                    software.amazon.awssdk.services.bedrock.model.Tag.builder()
                            .key(entry.getKey())
                            .value(entry.getValue())
                            .build();
            sdkTags.add(tag);
        }
        return sdkTags;
    }

    public static List<Tag> translateFromSdkTags(final List<software.amazon.awssdk.services.bedrock.model.Tag> tags) {
        return streamOfOrEmpty(tags)
                .map(tag -> Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toList());
    }

    private static String toStringIfNotNull(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toString();
    }
}
