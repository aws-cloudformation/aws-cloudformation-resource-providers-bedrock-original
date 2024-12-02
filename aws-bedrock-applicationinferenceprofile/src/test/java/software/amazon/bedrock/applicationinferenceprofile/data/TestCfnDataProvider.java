package software.amazon.bedrock.applicationinferenceprofile.data;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.services.bedrock.model.InferenceProfileStatus;
import software.amazon.bedrock.applicationinferenceprofile.InferenceProfileModel;
import software.amazon.bedrock.applicationinferenceprofile.InferenceProfileModelSource;
import software.amazon.bedrock.applicationinferenceprofile.ResourceModel;
import software.amazon.bedrock.applicationinferenceprofile.Tag;
import software.amazon.bedrock.applicationinferenceprofile.Translator;

import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.CREATED_AT;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.FOUNDATION_MODEL_ARN_PDX;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.INFERENCE_PROFILE_ARN;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.INFERENCE_PROFILE_DESCRIPTION;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.INFERENCE_PROFILE_ID;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.INFERENCE_PROFILE_NAME;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestConstants.UPDATED_AT;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_RESOURCE_TAG_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_RESOURCE_TAG_2;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_STACK_TAG_LIST_1;
import static software.amazon.bedrock.applicationinferenceprofile.data.TestSdkDataProvider.SDK_STACK_TAG_LIST_2;

/**
 * Provide test objects using CFN (CloudFormation) model
 */
public class TestCfnDataProvider {
    public static List<Tag> CFN_TAG_LIST_1 = Translator.translateFromSdkTags(SDK_STACK_TAG_LIST_1);

    public static ResourceModel INFERENCE_PROFILE_RESOURCE_MODEL_WITHOUT_RESOURCE_TAGS = ResourceModel.builder()
            .inferenceProfileIdentifier(INFERENCE_PROFILE_ARN)
            .inferenceProfileArn(INFERENCE_PROFILE_ARN)
            .inferenceProfileId(INFERENCE_PROFILE_ID)
            .inferenceProfileName(INFERENCE_PROFILE_NAME)
            .models(Collections.singletonList(InferenceProfileModel.builder().modelArn(FOUNDATION_MODEL_ARN_PDX).build()))
            .description(INFERENCE_PROFILE_DESCRIPTION)
            .createdAt(CREATED_AT.toString())
            .updatedAt(UPDATED_AT.toString())
            .status(InferenceProfileStatus.ACTIVE.toString())
            .build();

    public static ResourceModel INFERENCE_PROFILE_RESOURCE_MODEL = INFERENCE_PROFILE_RESOURCE_MODEL_WITHOUT_RESOURCE_TAGS.toBuilder()
            .tags(Translator.translateFromSdkTags(List.of(SDK_RESOURCE_TAG_1, SDK_RESOURCE_TAG_2)))
            .build();

    public static ResourceModel CFN_RESOURCE_MODEL_FOR_CREATE_REQUEST = ResourceModel.builder()
            .inferenceProfileName(INFERENCE_PROFILE_NAME)
            .description(INFERENCE_PROFILE_DESCRIPTION)
            .modelSource(InferenceProfileModelSource.builder().copyFrom(FOUNDATION_MODEL_ARN_PDX).build())
            .tags(CFN_TAG_LIST_1)
            .build();
}
