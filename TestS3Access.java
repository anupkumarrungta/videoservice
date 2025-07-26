import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import java.nio.charset.StandardCharsets;

public class TestS3Access {
    public static void main(String[] args) {
        try {
            // Create S3 client
            S3Client s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
            
            String bucketName = "video-translation-bucket2";
            String s3Key = "transcription-results/transcription-63c1063f.json";
            
            System.out.println("=== Testing S3 Access ===");
            System.out.println("Bucket: " + bucketName);
            System.out.println("Key: " + s3Key);
            System.out.println("S3 URI: s3://" + bucketName + "/" + s3Key);
            System.out.println("Object URL: https://" + bucketName + ".s3.us-east-1.amazonaws.com/" + s3Key);
            
            // Test 1: Check if file exists using HeadObject
            System.out.println("\n1. Testing HeadObject (file existence)...");
            try {
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
                
                var headResponse = s3Client.headObject(headRequest);
                System.out.println("✓ File exists!");
                System.out.println("  - Size: " + headResponse.contentLength() + " bytes");
                System.out.println("  - ETag: " + headResponse.eTag());
                System.out.println("  - Content-Type: " + headResponse.contentType());
                System.out.println("  - Last Modified: " + headResponse.lastModified());
            } catch (Exception e) {
                System.out.println("✗ HeadObject failed: " + e.getMessage());
                return;
            }
            
            // Test 2: Download and read file content
            System.out.println("\n2. Testing GetObject (file download)...");
            try {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
                
                ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest);
                String content = new String(response.readAllBytes(), StandardCharsets.UTF_8);
                
                System.out.println("✓ File downloaded successfully!");
                System.out.println("  - Content length: " + content.length() + " characters");
                System.out.println("  - First 200 characters: " + content.substring(0, Math.min(200, content.length())));
                
                // Check if it contains transcript
                if (content.contains("\"transcript\":")) {
                    System.out.println("  - Contains transcript field: ✓");
                    int start = content.indexOf("\"transcript\":") + 13;
                    int end = content.indexOf("\"", start);
                    if (end > start) {
                        String transcript = content.substring(start, end).trim();
                        System.out.println("  - Transcript: " + transcript);
                    }
                } else {
                    System.out.println("  - Contains transcript field: ✗");
                }
                
            } catch (Exception e) {
                System.out.println("✗ GetObject failed: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Test 3: Test with different URI formats
            System.out.println("\n3. Testing URI format parsing...");
            String[] testUris = {
                "https://s3.us-east-1.amazonaws.com/video-translation-bucket2/transcription-results/transcription-63c1063f.json",
                "https://video-translation-bucket2.s3.us-east-1.amazonaws.com/transcription-results/transcription-63c1063f.json",
                "s3://video-translation-bucket2/transcription-results/transcription-63c1063f.json"
            };
            
            for (String testUri : testUris) {
                System.out.println("\nTesting URI: " + testUri);
                String extractedKey = extractS3Key(testUri, bucketName);
                System.out.println("  - Extracted key: " + extractedKey);
                
                if (extractedKey != null) {
                    try {
                        HeadObjectRequest testRequest = HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(extractedKey)
                            .build();
                        
                        var testResponse = s3Client.headObject(testRequest);
                        System.out.println("  - ✓ Accessible (size: " + testResponse.contentLength() + " bytes)");
                    } catch (Exception e) {
                        System.out.println("  - ✗ Not accessible: " + e.getMessage());
                    }
                }
            }
            
            s3Client.close();
            System.out.println("\n=== Test Complete ===");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String extractS3Key(String transcriptUri, String bucketName) {
        System.out.println("  - URI analysis:");
        System.out.println("    Contains '.s3.amazonaws.com/': " + transcriptUri.contains(".s3.amazonaws.com/"));
        System.out.println("    Contains 's3.': " + transcriptUri.contains("s3."));
        System.out.println("    Contains bucket name: " + transcriptUri.contains(bucketName));
        
        if (transcriptUri.contains(".s3.amazonaws.com/")) {
            // Handle format: https://bucket.s3.amazonaws.com/key
            String key = transcriptUri.replace("https://" + bucketName + ".s3.amazonaws.com/", "");
            System.out.println("    Using bucket format, extracted key: " + key);
            return key;
        } else if (transcriptUri.contains("s3.")) {
            // Handle region-specific URLs like https://s3.us-east-1.amazonaws.com/bucket/key
            String[] uriParts = transcriptUri.split("/");
            System.out.println("    URI parts count: " + uriParts.length);
            
            int bucketIndex = -1;
            for (int i = 0; i < uriParts.length; i++) {
                if (bucketName.equals(uriParts[i])) {
                    bucketIndex = i;
                    break;
                }
            }
            
            System.out.println("    Bucket found at index: " + bucketIndex);
            
            if (bucketIndex != -1 && bucketIndex + 1 < uriParts.length) {
                StringBuilder keyBuilder = new StringBuilder();
                for (int i = bucketIndex + 1; i < uriParts.length; i++) {
                    if (i > bucketIndex + 1) keyBuilder.append("/");
                    keyBuilder.append(uriParts[i]);
                }
                String key = keyBuilder.toString();
                System.out.println("    Using path-style format, extracted key: " + key);
                return key;
            }
        } else if (transcriptUri.startsWith("s3://")) {
            // Handle S3 URI format: s3://bucket/key
            String key = transcriptUri.replace("s3://" + bucketName + "/", "");
            System.out.println("    Using S3 URI format, extracted key: " + key);
            return key;
        }
        
        System.out.println("    Could not extract key from URI");
        return null;
    }
} 