public class TestUriParsing {
    public static void main(String[] args) {
        String transcriptUri = "https://s3.us-east-1.amazonaws.com/video-translation-bucket2/transcription-results/transcription-ea280941.json";
        String bucketName = "video-translation-bucket2";
        
        System.out.println("Testing URI: " + transcriptUri);
        System.out.println("URI length: " + transcriptUri.length());
        System.out.println("Contains '.s3.amazonaws.com/': " + transcriptUri.contains(".s3.amazonaws.com/"));
        System.out.println("Contains '.s3.': " + transcriptUri.contains(".s3."));
        System.out.println("Contains 's3.': " + transcriptUri.contains("s3."));
        System.out.println("Contains bucket name: " + transcriptUri.contains(bucketName));
        
        // Check each character around the s3 part
        int s3Index = transcriptUri.indexOf("s3.");
        if (s3Index != -1) {
            System.out.println("Found 's3.' at index: " + s3Index);
            System.out.println("Character before 's3.': '" + transcriptUri.charAt(s3Index - 1) + "'");
            System.out.println("Characters around 's3.': '" + transcriptUri.substring(Math.max(0, s3Index - 2), Math.min(transcriptUri.length(), s3Index + 5)) + "'");
        }
        
        if (transcriptUri.contains(".s3.amazonaws.com/")) {
            System.out.println("Would use bucket format");
        } else if (transcriptUri.contains(".s3.")) {
            System.out.println("Would use path-style format");
        } else if (transcriptUri.contains("s3.")) {
            System.out.println("Would use s3. format");
            
            String[] uriParts = transcriptUri.split("/");
            System.out.println("URI parts count: " + uriParts.length);
            for (int i = 0; i < uriParts.length; i++) {
                System.out.println("Part " + i + ": '" + uriParts[i] + "'");
            }
            
            int bucketIndex = -1;
            for (int i = 0; i < uriParts.length; i++) {
                if (bucketName.equals(uriParts[i])) {
                    bucketIndex = i;
                    break;
                }
            }
            
            System.out.println("Bucket '" + bucketName + "' found at index: " + bucketIndex);
            
            if (bucketIndex != -1 && bucketIndex + 1 < uriParts.length) {
                StringBuilder keyBuilder = new StringBuilder();
                for (int i = bucketIndex + 1; i < uriParts.length; i++) {
                    if (i > bucketIndex + 1) keyBuilder.append("/");
                    keyBuilder.append(uriParts[i]);
                }
                String s3Key = keyBuilder.toString();
                System.out.println("Extracted key: " + s3Key);
            } else {
                System.out.println("Could not extract key");
            }
        } else {
            System.out.println("Would throw unsupported format error");
        }
    }
} 