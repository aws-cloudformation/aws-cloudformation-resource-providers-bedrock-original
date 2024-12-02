package software.amazon.bedrock.applicationinferenceprofile.data;

import java.time.Instant;

public class TestConstants {
    public static String INFERENCE_PROFILE_SUMMARY_DIFFERENTIATOR = "INFERENCE_PROFILE_SUMMARY_DIFFERENTIATOR";
    public static Instant CREATED_AT = Instant.now().minusSeconds(3600);
    public static Instant UPDATED_AT = Instant.now();
    public static int TIME_DELTA = 1;
    public static String INFERENCE_PROFILE_ARN = "INFERENCE_PROFILE_ARN";
    public static String APPLICATION_INFERENCE_PROFILE_ARN =
            "arn:aws:bedrock:us-west-2:575108918522:application-inference-profile/6k6sikjfma06";
    public static String CLIENT_REQUEST_TOKEN = "CLIENT_REQUEST_TOKEN";
    public static String INFERENCE_PROFILE_DESCRIPTION = "INFERENCE_PROFILE_DESCRIPTION";
    public static String INFERENCE_PROFILE_NAME = "INFERENCE_PROFILE_NAME";
    public static String INFERENCE_PROFILE_ID = "INFERENCE_PROFILE_ID";
    public static String FOUNDATION_MODEL_ARN_PDX =
            "arn:aws:bedrock:us-west-2::foundation-model/anthropic.claude-3-5-sonnet-20240620-v1:0";
    public static String FOUNDATION_MODEL_ARN_IAD =
            "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-5-sonnet-20240620-v1:0";
    public static String FOUNDATION_MODEL_ARN_2 = "FOUNDATION_MODEL_ARN_2";
    public static String TAG_KEY_1 = "TAG_KEY_1";
    public static String TAG_VALUE_1 = "TAG_VALUE_1";
    public static String TAG_KEY_2 = "TAG_KEY_2";
    public static String TAG_VALUE_2 = "TAG_VALUE_2";
    public static String RESOURCE_TAG_KEY_1 = "RESOURCE_TAG_KEY_1";
    public static String RESOURCE_TAG_VALUE_1 = "RESOURCE_TAG_VALUE_1";
    public static String RESOURCE_TAG_KEY_2 = "RESOURCE_TAG_KEY_2";
    public static String RESOURCE_TAG_VALUE_2 = "RESOURCE_TAG_VALUE_2";
}
