package com.videoservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling text translation using AWS Translate.
 * Preserves context and emotional tone during translation.
 */
@Service
public class TranslationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
    
    private final TranslateClient translateClient;
    
    // Language code mappings
    private static final Map<String, String> LANGUAGE_CODES = new ConcurrentHashMap<>();
    
    static {
        // English
        LANGUAGE_CODES.put("english", "en");
        
        // Indian Languages
        LANGUAGE_CODES.put("hindi", "hi");
        LANGUAGE_CODES.put("tamil", "ta");
        LANGUAGE_CODES.put("telugu", "te");
        LANGUAGE_CODES.put("kannada", "kn");
        LANGUAGE_CODES.put("malayalam", "ml");
        LANGUAGE_CODES.put("bengali", "bn");
        LANGUAGE_CODES.put("marathi", "mr");
        LANGUAGE_CODES.put("gujarati", "gu");
        LANGUAGE_CODES.put("punjabi", "pa");
        LANGUAGE_CODES.put("urdu", "ur");
        
        // Other Languages
        LANGUAGE_CODES.put("arabic", "ar");
        LANGUAGE_CODES.put("korean", "ko");
        LANGUAGE_CODES.put("chinese", "zh");
        LANGUAGE_CODES.put("spanish", "es");
        LANGUAGE_CODES.put("french", "fr");
        LANGUAGE_CODES.put("german", "de");
        LANGUAGE_CODES.put("japanese", "ja");
    }
    
    @Value("${translation.aws.source-language:english}")
    private String defaultSourceLanguage;
    
    // Map to store proper noun replacements during translation
    private Map<String, String> properNounMap = new HashMap<>();
    
    public TranslationService(TranslateClient translateClient) {
        this.translateClient = translateClient;
    }
    
    /**
     * Translate text from source language to target language.
     * Handles long text by chunking it into smaller pieces.
     * 
     * @param text The text to translate
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return The translated text
     * @throws TranslationException if translation fails
     */
    public String translateText(String text, String sourceLanguage, String targetLanguage) throws TranslationException {
        logger.info("[Translation Service] Starting translation from {} to {}: {} chars", sourceLanguage, targetLanguage, text.length());
        logger.info("[Translation Service] Source text: {}", text);
        
        try {
            // Validate language support first
            String sourceCode = getLanguageCode(sourceLanguage);
            String targetCode = getLanguageCode(targetLanguage);
            
            if (sourceCode == null) {
                throw new TranslationException("Unsupported source language: " + sourceLanguage);
            }
            if (targetCode == null) {
                throw new TranslationException("Unsupported target language: " + targetLanguage);
            }
            
            logger.info("[Translation Service] Language codes - Source: {}, Target: {}", sourceCode, targetCode);
            
            // If text is too long, chunk it into smaller pieces
            if (text.length() > 5000) {
                logger.info("[Translation Service] Text is too long ({} chars), chunking for translation", text.length());
                return translateLongText(text, sourceLanguage, targetLanguage);
            }
            
            // Reset proper noun map for this translation
            properNounMap.clear();
            
            // Extract and preserve proper nouns (names, places, etc.)
            String processedText = preserveProperNouns(text);
            logger.info("[Translation Service] Processed text with preserved proper nouns: {}", processedText);
            
            // Check if the language pair is directly supported
            if (!isLanguagePairSupported(sourceLanguage, targetLanguage)) {
                logger.info("[Translation Service] Language pair not directly supported, using English as intermediate");
                return translateViaEnglish(processedText, sourceLanguage, targetLanguage);
            }
            
            TranslateTextRequest request = TranslateTextRequest.builder()
                    .text(processedText)
                    .sourceLanguageCode(sourceCode)
                    .targetLanguageCode(targetCode)
                    .build();
            
            logger.info("[Translation Service] Sending translation request to AWS Translate...");
            TranslateTextResponse response = translateClient.translateText(request);
            
            String translatedText = response.translatedText();
            
            // Restore proper nouns in the translated text
            String finalText = restoreProperNouns(translatedText, text);
            
            logger.info("[Translation Service] Translation completed successfully!");
            logger.info("[Translation Service] Original text ({} chars): {}", text.length(), text);
            logger.info("[Translation Service] Translated text ({} chars): {}", finalText.length(), finalText);
            
            return finalText;
            
        } catch (TranslateException e) {
            logger.error("Translation failed: {}", e.getMessage());
            
            // Check if it's an unsupported language pair
            if (e.getMessage().contains("Unsupported language pair")) {
                logger.warn("Unsupported language pair: {} to {}. Attempting fallback translation via English", sourceLanguage, targetLanguage);
                return translateViaEnglish(text, sourceLanguage, targetLanguage);
            }
            
            throw new TranslationException("Failed to translate text", e);
        }
    }
    
    /**
     * Translate long text by breaking it into chunks.
     * 
     * @param text The long text to translate
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return The translated text
     * @throws TranslationException if translation fails
     */
    private String translateLongText(String text, String sourceLanguage, String targetLanguage) throws TranslationException {
        logger.info("Translating long text in chunks: {} chars", text.length());
        
        // Reset proper noun map for this translation
        properNounMap.clear();
        
        // Split text into sentences or chunks
        String[] sentences = text.split("[।.!?]");
        StringBuilder translatedText = new StringBuilder();
        
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (sentence.isEmpty()) {
                continue;
            }
            
            try {
                // Add punctuation back if it was removed
                String sentenceToTranslate = sentence;
                if (i < sentences.length - 1) {
                    sentenceToTranslate += "।"; // Add Hindi period back
                }
                
                String translatedSentence = translateTextChunk(sentenceToTranslate, sourceLanguage, targetLanguage);
                translatedText.append(translatedSentence).append(" ");
                
                logger.debug("Translated chunk {}/{}: {} chars", i + 1, sentences.length, sentenceToTranslate.length());
                
                // Small delay to avoid rate limiting
                Thread.sleep(100);
                
            } catch (Exception e) {
                logger.warn("Failed to translate chunk {}, using original: {}", i + 1, e.getMessage());
                translatedText.append(sentence).append(" ");
            }
        }
        
        logger.info("Long text translation completed: {} -> {} chars", text.length(), translatedText.length());
        return translatedText.toString().trim();
    }
    
    /**
     * Translate a single text chunk (internal method to avoid recursion).
     * 
     * @param text The text chunk to translate
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return The translated text
     * @throws TranslationException if translation fails
     */
    private String translateTextChunk(String text, String sourceLanguage, String targetLanguage) throws TranslationException {
        try {
            String sourceCode = getLanguageCode(sourceLanguage);
            String targetCode = getLanguageCode(targetLanguage);
            
            // Preserve proper nouns in this chunk
            String processedText = preserveProperNouns(text);
            
            TranslateTextRequest request = TranslateTextRequest.builder()
                    .text(processedText)
                    .sourceLanguageCode(sourceCode)
                    .targetLanguageCode(targetCode)
                    .build();
            
            TranslateTextResponse response = translateClient.translateText(request);
            String translatedText = response.translatedText();
            
            // Restore proper nouns in the translated text
            String finalText = restoreProperNouns(translatedText, text);
            
            return finalText;
            
        } catch (TranslateException e) {
            logger.error("Translation chunk failed: {}", e.getMessage());
            throw new TranslationException("Failed to translate text chunk", e);
        }
    }
    
    /**
     * Translate multiple texts in batch.
     * 
     * @param texts List of texts to translate
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return List of translated texts
     * @throws TranslationException if translation fails
     */
    public List<String> translateBatch(List<String> texts, String sourceLanguage, String targetLanguage) throws TranslationException {
        logger.info("Translating batch of {} texts from {} to {}", texts.size(), sourceLanguage, targetLanguage);
        
        try {
            String sourceCode = getLanguageCode(sourceLanguage);
            String targetCode = getLanguageCode(targetLanguage);
            
            // Note: Batch translation requires S3 setup and IAM roles
            // For now, we'll use individual translations
            // BatchTranslateTextRequest is not available in AWS SDK v2
            
            // Note: Batch translation requires S3 setup and IAM roles
            // For now, we'll use individual translations
            return texts.stream()
                    .map(text -> {
                        try {
                            return translateText(text, sourceLanguage, targetLanguage);
                        } catch (TranslationException e) {
                            logger.error("Failed to translate text in batch: {}", e.getMessage());
                            return text; // Return original text on failure
                        }
                    })
                    .toList();
            
        } catch (Exception e) {
            logger.error("Batch translation failed: {}", e.getMessage());
            throw new TranslationException("Failed to perform batch translation", e);
        }
    }
    
    /**
     * Translate text with context preservation.
     * 
     * @param text The text to translate
     * @param context The context information
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return The translated text with context preserved
     * @throws TranslationException if translation fails
     */
    public String translateWithContext(String text, String context, String sourceLanguage, String targetLanguage) throws TranslationException {
        logger.debug("Translating text with context from {} to {}", sourceLanguage, targetLanguage);
        
        // Combine context and text for better translation
        String contextualizedText = String.format("Context: %s\nText: %s", context, text);
        
        String translatedText = translateText(contextualizedText, sourceLanguage, targetLanguage);
        
        // Extract the translated text part (remove context prefix)
        if (translatedText.contains("Text:")) {
            translatedText = translatedText.substring(translatedText.indexOf("Text:") + 5).trim();
        }
        
        return translatedText;
    }
    
    /**
     * Detect the language of the input text.
     * 
     * @param text The text to detect language for
     * @return The detected language code
     * @throws TranslationException if detection fails
     */
    public String detectLanguage(String text) throws TranslationException {
        logger.debug("Detecting language for text: {}", text.substring(0, Math.min(50, text.length())));
        
        try {
            // AWS Translate doesn't have a direct detectLanguage method in SDK v2
            // We'll use a simple heuristic based on common patterns
            // For production, consider using AWS Comprehend for language detection
            
            String detectedLanguage = detectLanguageHeuristic(text);
            
            logger.debug("Language detected: {} for text", detectedLanguage);
            
            return detectedLanguage;
            
        } catch (TranslateException e) {
            logger.error("Language detection failed: {}", e.getMessage());
            throw new TranslationException("Failed to detect language", e);
        }
    }
    
    /**
     * Get supported languages.
     * 
     * @return List of supported language codes
     * @throws TranslationException if retrieval fails
     */
    public List<String> getSupportedLanguages() throws TranslationException {
        logger.debug("Retrieving supported languages");
        
        try {
            ListLanguagesRequest request = ListLanguagesRequest.builder().build();
            ListLanguagesResponse response = translateClient.listLanguages(request);
            
            List<String> languages = response.languages().stream()
                    .map(Language::languageCode)
                    .toList();
            
            logger.debug("Retrieved {} supported languages", languages.size());
            
            return languages;
            
        } catch (TranslateException e) {
            logger.error("Failed to retrieve supported languages: {}", e.getMessage());
            throw new TranslationException("Failed to retrieve supported languages", e);
        }
    }
    
    /**
     * Get language code from language name.
     * 
     * @param languageName The language name
     * @return The language code
     */
    private String getLanguageCode(String languageName) {
        String code = LANGUAGE_CODES.get(languageName.toLowerCase());
        if (code == null) {
            logger.warn("Unsupported language: {}", languageName);
            return null; // Return null for unsupported languages
        }
        return code;
    }
    
    /**
     * Get language name from language code.
     * 
     * @param languageCode The language code
     * @return The language name
     */
    public String getLanguageName(String languageCode) {
        return LANGUAGE_CODES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(languageCode))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(languageCode);
    }
    
    /**
     * Simple heuristic language detection.
     * 
     * @param text The text to analyze
     * @return The detected language code
     */
    private String detectLanguageHeuristic(String text) {
        // Enhanced character-based detection for Indian languages
        if (text.matches(".*[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF].*")) {
            return "ar"; // Arabic
        } else if (text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF].*")) {
            return "zh"; // Chinese
        } else if (text.matches(".*[\\uAC00-\\uD7AF].*")) {
            return "ko"; // Korean
        } else if (text.matches(".*[\\u0B80-\\u0BFF].*")) {
            return "ta"; // Tamil
        } else if (text.matches(".*[\\u0C00-\\u0C7F].*")) {
            return "te"; // Telugu
        } else if (text.matches(".*[\\u0C80-\\u0CFF].*")) {
            return "kn"; // Kannada
        } else if (text.matches(".*[\\u0D00-\\u0D7F].*")) {
            return "ml"; // Malayalam
        } else if (text.matches(".*[\\u0980-\\u09FF].*")) {
            return "bn"; // Bengali
        } else if (text.matches(".*[\\u0A80-\\u0AFF].*")) {
            return "gu"; // Gujarati
        } else if (text.matches(".*[\\u0A00-\\u0A7F].*")) {
            return "pa"; // Punjabi (Gurmukhi)
        } else if (text.matches(".*[\\u0900-\\u097F].*")) {
            return "hi"; // Hindi/Devanagari (covers Hindi, Marathi, etc.)
        } else if (text.matches(".*[\\u0620-\\u063F\\u0641-\\u064A\\u0660-\\u0669].*")) {
            return "ur"; // Urdu
        } else {
            // Check for common English words and patterns
            String lowerText = text.toLowerCase();
            if (lowerText.matches(".*\\b(the|and|or|but|in|on|at|to|for|of|with|by|is|are|was|were|be|been|being|have|has|had|do|does|did|will|would|could|should|may|might|can|must|shall)\\b.*")) {
                return "en"; // English
            } else {
                // If no clear pattern, default to English but log for analysis
                logger.warn("[Language Detection] No clear language pattern detected, defaulting to English. Text sample: {}", 
                           text.length() > 50 ? text.substring(0, 50) + "..." : text);
                return "en"; // Default to English
            }
        }
    }
    
    /**
     * Validate if a language is supported.
     * 
     * @param language The language to validate
     * @return true if supported, false otherwise
     */
    public boolean isLanguageSupported(String language) {
        return LANGUAGE_CODES.containsKey(language.toLowerCase());
    }
    
    /**
     * Check if a language pair is supported for direct translation.
     * 
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return true if the language pair is supported, false otherwise
     */
    public boolean isLanguagePairSupported(String sourceLanguage, String targetLanguage) {
        // Common supported language pairs for Indian languages
        String[] supportedPairs = {
            "en-hi", "hi-en",    // English-Hindi
            "en-ta", "ta-en",    // English-Tamil
            "en-te", "te-en",    // English-Telugu
            "en-kn", "kn-en",    // English-Kannada
            "en-ml", "ml-en",    // English-Malayalam
            "en-bn", "bn-en",    // English-Bengali
            "en-mr", "mr-en",    // English-Marathi
            "en-gu", "gu-en",    // English-Gujarati
            "en-pa", "pa-en",    // English-Punjabi
            "en-ur", "ur-en",    // English-Urdu
            // Note: Odia (or) and Assamese (as) have limited support
        };
        
        String sourceCode = getLanguageCode(sourceLanguage);
        String targetCode = getLanguageCode(targetLanguage);
        String pair = sourceCode + "-" + targetCode;
        
        for (String supportedPair : supportedPairs) {
            if (supportedPair.equals(pair)) {
                return true;
            }
        }
        
        logger.warn("[Translation Service] Language pair not in supported list: {} to {}", sourceLanguage, targetLanguage);
        return false;
    }
    
    /**
     * Preserve proper nouns (names, places, etc.) during translation by replacing them with unique identifiers.
     * This helps prevent translation services from translating proper nouns.
     */
    private String preserveProperNouns(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        try {
            // Split text into words
            String[] words = text.split("\\s+");
            StringBuilder processedText = new StringBuilder();
            Map<String, String> properNounMap = new HashMap<>();
            int properNounCounter = 0;
            
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                
                // Check if word looks like a proper noun (name, place, etc.)
                if (isProperNoun(word)) {
                    // Create a unique identifier for this proper noun
                    String identifier = "PN_" + properNounCounter + "_" + word.length();
                    properNounMap.put(identifier, word);
                    processedText.append(identifier);
                    logger.debug("[Translation Service] Preserved proper noun: {} -> {}", word, identifier);
                    properNounCounter++;
                } else {
                    processedText.append(word);
                }
                
                // Add space between words (except for the last word)
                if (i < words.length - 1) {
                    processedText.append(" ");
                }
            }
            
            // Store the mapping for restoration
            this.properNounMap = properNounMap;
            
            return processedText.toString();
            
        } catch (Exception e) {
            logger.warn("[Translation Service] Error preserving proper nouns: {}", e.getMessage());
            return text; // Return original text if processing fails
        }
    }
    
    /**
     * Restore proper nouns after translation by replacing identifiers with original words.
     */
    private String restoreProperNouns(String translatedText, String originalText) {
        if (translatedText == null || originalText == null) {
            return translatedText;
        }
        
        try {
            String restoredText = translatedText;
            
            // Restore proper nouns using the stored mapping
            for (Map.Entry<String, String> entry : properNounMap.entrySet()) {
                String identifier = entry.getKey();
                String properNoun = entry.getValue();
                
                // Replace the identifier with the original proper noun
                restoredText = restoredText.replace(identifier, properNoun);
                logger.debug("[Translation Service] Restored proper noun: {} -> {}", identifier, properNoun);
            }
            
            // Clear the mapping for next use
            properNounMap.clear();
            
            return restoredText;
            
        } catch (Exception e) {
            logger.warn("[Translation Service] Error restoring proper nouns: {}", e.getMessage());
            return translatedText; // Return translated text if restoration fails
        }
    }
    
    /**
     * Check if a word is likely a proper noun (name, place, etc.).
     */
    private boolean isProperNoun(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        
        String cleanWord = word.trim();
        
        // Skip common English words that shouldn't be preserved
        String[] commonWords = {
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "from", "up", "down", "out", "off", "over", "under",
            "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might", "can",
            "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them", "my", "your", "his", "her", "its", "our", "their",
            "very", "good", "morning", "everyone", "welcome", "back", "center", "stage", "host", "session", "high", "performance", "grace", "skill", "artistry", "display", "performers", "journey", "moment", "entertainment", "pause", "festivities", "continues"
        };
        
        String lowerWord = cleanWord.toLowerCase();
        for (String commonWord : commonWords) {
            if (lowerWord.equals(commonWord)) {
                return false;
            }
        }
        
        // Check for common proper noun patterns
        return (
            // Capitalized words that are likely names (not sentence starters)
            (cleanWord.length() > 1 && Character.isUpperCase(cleanWord.charAt(0)) && 
             !isCommonSentenceStarter(cleanWord)) ||
            
            // Acronyms (all caps, 2+ characters)
            cleanWord.matches("\\b[A-Z]{2,}\\b") ||
            
            // Words with mixed case (like "McDonald", "iPhone")
            cleanWord.matches("\\b[A-Z][a-z]*[A-Z][a-z]*\\b") ||
            
            // Specific name patterns (first letter capitalized, reasonable length)
            (cleanWord.length() >= 3 && cleanWord.length() <= 15 && 
             Character.isUpperCase(cleanWord.charAt(0)) && 
             cleanWord.matches("[A-Z][a-z]+") &&
             !isCommonWord(cleanWord))
        );
    }
    
    /**
     * Check if a word is a common sentence starter that shouldn't be preserved.
     */
    private boolean isCommonSentenceStarter(String word) {
        String[] starters = {"The", "A", "An", "This", "That", "These", "Those", "I", "You", "He", "She", "It", "We", "They"};
        for (String starter : starters) {
            if (word.equals(starter)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a word is a common English word that shouldn't be preserved.
     */
    private boolean isCommonWord(String word) {
        String[] commonWords = {
            "Very", "Good", "Morning", "Everyone", "Welcome", "Back", "Center", "Stage", "Host", "Session", "High", "Performance", "Grace", "Skill", "Artistry", "Display", "Performers", "Journey", "Moment", "Entertainment", "Pause", "Festivities", "Continues",
            "Between", "Captivating", "Refined", "Keeping", "Enthralled", "Transit", "While", "Take", "Offer", "As", "By", "On", "In", "At", "To", "For", "Of", "With", "From", "Up", "Down", "Out", "Off", "Over", "Under"
        };
        for (String commonWord : commonWords) {
            if (word.equals(commonWord)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Custom exception for translation errors.
     */
    public static class TranslationException extends Exception {
        public TranslationException(String message) {
            super(message);
        }
        
        public TranslationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Translate text via English as an intermediate step for unsupported language pairs.
     * This is a fallback method when direct translation is not supported.
     * 
     * @param text The text to translate
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return The translated text
     * @throws TranslationException if translation fails
     */
    private String translateViaEnglish(String text, String sourceLanguage, String targetLanguage) throws TranslationException {
        logger.info("[Translation Service] Using English as intermediate language for {} to {}", sourceLanguage, targetLanguage);
        
        try {
            // Step 1: Translate from source language to English
            String sourceCode = getLanguageCode(sourceLanguage);
            String englishCode = "en";
            
            logger.info("[Translation Service] Step 1: Translating {} to English", sourceLanguage);
            TranslateTextRequest request1 = TranslateTextRequest.builder()
                    .text(text)
                    .sourceLanguageCode(sourceCode)
                    .targetLanguageCode(englishCode)
                    .build();
            
            TranslateTextResponse response1 = translateClient.translateText(request1);
            String englishText = response1.translatedText();
            logger.info("[Translation Service] Step 1 completed: {} -> English", sourceLanguage);
            
            // Step 2: Translate from English to target language
            String targetCode = getLanguageCode(targetLanguage);
            
            logger.info("[Translation Service] Step 2: Translating English to {}", targetLanguage);
            TranslateTextRequest request2 = TranslateTextRequest.builder()
                    .text(englishText)
                    .sourceLanguageCode(englishCode)
                    .targetLanguageCode(targetCode)
                    .build();
            
            TranslateTextResponse response2 = translateClient.translateText(request2);
            String translatedText = response2.translatedText();
            logger.info("[Translation Service] Step 2 completed: English -> {}", targetLanguage);
            
            // Restore proper nouns in the final translated text
            String finalText = restoreProperNouns(translatedText, text);
            
            logger.info("[Translation Service] Fallback translation completed successfully!");
            logger.info("[Translation Service] Original: {} chars, English: {} chars, Final: {} chars", 
                       text.length(), englishText.length(), finalText.length());
            
            return finalText;
            
        } catch (TranslateException e) {
            logger.error("[Translation Service] Fallback translation failed: {}", e.getMessage());
            if (e.getMessage().contains("UnsupportedLanguagePairException")) {
                throw new TranslationException("Language pair not supported by AWS Translate: " + sourceLanguage + " to " + targetLanguage, e);
            } else {
                throw new TranslationException("Fallback translation via English failed: " + e.getMessage(), e);
            }
        }
    }
} 