package com.videoservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.*;

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
        LANGUAGE_CODES.put("english", "en");
        LANGUAGE_CODES.put("arabic", "ar");
        LANGUAGE_CODES.put("korean", "ko");
        LANGUAGE_CODES.put("chinese", "zh");
        LANGUAGE_CODES.put("tamil", "ta");
        LANGUAGE_CODES.put("hindi", "hi");
        LANGUAGE_CODES.put("auto", "auto");
    }
    
    @Value("${translation.aws.source-language:auto}")
    private String defaultSourceLanguage;
    
    public TranslationService(TranslateClient translateClient) {
        this.translateClient = translateClient;
    }
    
    /**
     * Translate text from source language to target language.
     * 
     * @param text The text to translate
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return The translated text
     * @throws TranslationException if translation fails
     */
    public String translateText(String text, String sourceLanguage, String targetLanguage) throws TranslationException {
        logger.debug("Translating text from {} to {}: {}", sourceLanguage, targetLanguage, text.substring(0, Math.min(50, text.length())));
        
        try {
            String sourceCode = getLanguageCode(sourceLanguage);
            String targetCode = getLanguageCode(targetLanguage);
            
            TranslateTextRequest request = TranslateTextRequest.builder()
                    .text(text)
                    .sourceLanguageCode(sourceCode)
                    .targetLanguageCode(targetCode)
                    .build();
            
            TranslateTextResponse response = translateClient.translateText(request);
            
            String translatedText = response.translatedText();
            logger.debug("Translation completed: {} -> {}", text.substring(0, Math.min(30, text.length())), 
                        translatedText.substring(0, Math.min(30, translatedText.length())));
            
            return translatedText;
            
        } catch (TranslateException e) {
            logger.error("Translation failed: {}", e.getMessage());
            throw new TranslationException("Failed to translate text", e);
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
            logger.warn("Unknown language: {}, using default", languageName);
            return defaultSourceLanguage;
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
        // Simple character-based detection
        if (text.matches(".*[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF].*")) {
            return "ar"; // Arabic
        } else if (text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF].*")) {
            return "zh"; // Chinese
        } else if (text.matches(".*[\\uAC00-\\uD7AF].*")) {
            return "ko"; // Korean
        } else if (text.matches(".*[\\u0B80-\\u0BFF].*")) {
            return "ta"; // Tamil
        } else if (text.matches(".*[\\u0900-\\u097F].*")) {
            return "hi"; // Hindi
        } else {
            return "en"; // Default to English
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
} 