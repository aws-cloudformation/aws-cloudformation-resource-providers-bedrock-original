package software.amazon.bedrock.applicationinferenceprofile.data;

import java.util.List;
import java.util.stream.Stream;
import software.amazon.awssdk.services.bedrock.model.CreateInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.GetInferenceProfileResponse;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileModel;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileType;
import software.amazon.awssdk.services.bedrock.model.ListInferenceProfilesResponse;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileStatus;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileSummary;
import software.amazon.awssdk.services.bedrock.model.Tag;

import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.APPLICATION_INFERENCE_PROFILE_ARN;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.CREATED_AT;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.FOUNDATION_MODEL_ARN_IAD;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.FOUNDATION_MODEL_ARN_PDX;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.FOUNDATION_MODEL_ARN_2;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.INFERENCE_PROFILE_ARN;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.INFERENCE_PROFILE_DESCRIPTION;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.INFERENCE_PROFILE_ID;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.INFERENCE_PROFILE_NAME;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.INFERENCE_PROFILE_SUMMARY_DIFFERENTIATOR;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.RESOURCE_TAG_KEY_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.RESOURCE_TAG_KEY_2;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.RESOURCE_TAG_VALUE_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.RESOURCE_TAG_VALUE_2;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.TAG_KEY_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.TAG_KEY_2;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.TAG_VALUE_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.TAG_VALUE_2;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.TIME_DELTA;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.UPDATED_AT;

public class TestSdkDataProvider {
    public static InferenceProfileModel INFERENCE_PROFILE_MODEL_PDX = InferenceProfileModel.builder()
            .modelArn(FOUNDATION_MODEL_ARN_PDX)
            .build();

    public static InferenceProfileModel INFERENCE_PROFILE_MODEL_IAD = InferenceProfileModel.builder()
            .modelArn(FOUNDATION_MODEL_ARN_IAD)
            .build();

    public static InferenceProfileModel INFERENCE_PROFILE_MODEL_2 = InferenceProfileModel.builder()
            .modelArn(FOUNDATION_MODEL_ARN_2)
            .build();

    public static final CreateInferenceProfileResponse CREATE_INFERENCE_PROFILE_RESPONSE = CreateInferenceProfileResponse.builder()
            .inferenceProfileArn(INFERENCE_PROFILE_ARN)
            .status(InferenceProfileStatus.ACTIVE)
            .build();

    public static GetInferenceProfileResponse constructGetInferenceProfileResponse(final InferenceProfileStatus inferenceProfileStatus) {
        return GetInferenceProfileResponse.builder()
                .inferenceProfileArn(INFERENCE_PROFILE_ARN)
                .inferenceProfileId(INFERENCE_PROFILE_ID)
                .inferenceProfileName(INFERENCE_PROFILE_NAME)
                .models(List.of(INFERENCE_PROFILE_MODEL_IAD, INFERENCE_PROFILE_MODEL_PDX))
                .description(INFERENCE_PROFILE_DESCRIPTION)
                .status(inferenceProfileStatus)
                .type(InferenceProfileType.APPLICATION)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    private static final InferenceProfileSummary INFERENCE_PROFILE_SUMMARY1 = InferenceProfileSummary.builder()
            .inferenceProfileArn(INFERENCE_PROFILE_ARN)
            .inferenceProfileName(INFERENCE_PROFILE_NAME)
            .status(InferenceProfileStatus.ACTIVE)
            .type(InferenceProfileType.SYSTEM_DEFINED)
            .models(List.of(INFERENCE_PROFILE_MODEL_PDX, INFERENCE_PROFILE_MODEL_IAD))
            .description(INFERENCE_PROFILE_DESCRIPTION)
            .createdAt(CREATED_AT)
            .updatedAt(UPDATED_AT)
            .build();

    private static final InferenceProfileSummary INFERENCE_PROFILE_SUMMARY2 = InferenceProfileSummary.builder()
            .inferenceProfileArn(INFERENCE_PROFILE_ARN + INFERENCE_PROFILE_SUMMARY_DIFFERENTIATOR)
            .inferenceProfileName(INFERENCE_PROFILE_NAME + INFERENCE_PROFILE_SUMMARY_DIFFERENTIATOR)
            .status(InferenceProfileStatus.ACTIVE)
            .type(InferenceProfileType.SYSTEM_DEFINED)
            .description(INFERENCE_PROFILE_DESCRIPTION + INFERENCE_PROFILE_SUMMARY_DIFFERENTIATOR)
            .description(INFERENCE_PROFILE_DESCRIPTION + INFERENCE_PROFILE_SUMMARY_DIFFERENTIATOR)
            .createdAt(CREATED_AT.plusSeconds(TIME_DELTA))
            .updatedAt(UPDATED_AT.minusSeconds(TIME_DELTA))
            .build();

    private static final InferenceProfileSummary APPLICATION_INFERENCE_PROFILE_SUMMARY = INFERENCE_PROFILE_SUMMARY1.toBuilder()
            .inferenceProfileArn(APPLICATION_INFERENCE_PROFILE_ARN)
            .type(InferenceProfileType.APPLICATION)
            .build();

    public static ListInferenceProfilesResponse LIST_INFERENCE_PROFILES_RESPONSE = ListInferenceProfilesResponse.builder()
            .inferenceProfileSummaries(List.of(INFERENCE_PROFILE_SUMMARY1, INFERENCE_PROFILE_SUMMARY2))
            .build();

    public static ListInferenceProfilesResponse LIST_APPLICATION_INFERENCE_PROFILES_RESPONSE =
            ListInferenceProfilesResponse.builder()
            .inferenceProfileSummaries(List.of(APPLICATION_INFERENCE_PROFILE_SUMMARY))
            .build();

    public static Tag SDK_STACK_TAG_1 = Tag.builder()
            .key(TAG_KEY_1)
            .value(TAG_VALUE_1)
            .build();

    public static Tag SDK_STACK_TAG_2 = Tag.builder()
            .key(TAG_KEY_2)
            .value(TAG_VALUE_2)
            .build();

    public static List<Tag> SDK_STACK_TAG_LIST_1 = Stream.of(SDK_STACK_TAG_1, SDK_STACK_TAG_2)
            .toList();

    public static List<Tag> SDK_STACK_TAG_LIST_2 = Stream.of(SDK_STACK_TAG_1)
            .toList();

    public static Tag SDK_RESOURCE_TAG_1 = Tag.builder()
            .key(RESOURCE_TAG_KEY_1)
            .value(RESOURCE_TAG_VALUE_1)
            .build();

    public static Tag SDK_RESOURCE_TAG_2 = Tag.builder()
            .key(RESOURCE_TAG_KEY_2)
            .value(RESOURCE_TAG_VALUE_2)
            .build();

    public static List<InferenceProfileModel> SDK_INFERENCE_PROFILE_MODEL_LIST = Stream.of(INFERENCE_PROFILE_MODEL_PDX, INFERENCE_PROFILE_MODEL_2)
            .toList();
}
