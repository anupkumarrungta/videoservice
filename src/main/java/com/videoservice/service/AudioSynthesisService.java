package com.videoservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for text-to-speech synthesis using AWS Polly.
 * Generates natural-sounding speech in target languages.
 */
@Service
public class AudioSynthesisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioSynthesisService.class);
    
    private final PollyClient pollyClient;
    
    // Voice mappings for different languages
    private static final Map<String, String> VOICE_MAPPINGS = new ConcurrentHashMap<>();
    
    static {
        VOICE_MAPPINGS.put("english", "Joanna");
        VOICE_MAPPINGS.put("arabic", "Zeina");
        VOICE_MAPPINGS.put("korean", "Seoyeon");
        VOICE_MAPPINGS.put("chinese", "Zhiyu");
        VOICE_MAPPINGS.put("tamil", "Raveena");
        VOICE_MAPPINGS.put("hindi", "Aditi");
    }
    
    @Value("${tts.aws.engine:neural}")
    private String engine;
    
    @Value("${audio.sample-rate:16000}")
    private int sampleRate;
    
    @Value("${audio.bit-rate:128000}")
    private int bitRate;
    
    public AudioSynthesisService(PollyClient pollyClient) {
        this.pollyClient = pollyClient;
    }
    
    /**
     * Synthesize speech from text.
     * 
     * @param text The text to synthesize
     * @param language The target language
     * @param outputFile The output audio file
     * @return The synthesized audio file
     * @throws SynthesisException if synthesis fails
     */
    public File synthesizeSpeech(String text, String language, File outputFile) throws SynthesisException {
        logger.info("Synthesizing speech for language: {} to file: {}", language, outputFile.getName());
        
        try {
            String voiceId = getVoiceId(language);
            
            SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                    .text(text)
                    .voiceId(voiceId)
                    .outputFormat(OutputFormat.MP3)
                    .engine(Engine.NEURAL)
                    .sampleRate(String.valueOf(sampleRate))
                    .build();
            
            ResponseInputStream<SynthesizeSpeechResponse> response = pollyClient.synthesizeSpeech(request);
            
            // Write audio data to file
            byte[] audioData = response.readAllBytes();
            Files.write(outputFile.toPath(), audioData);
            
            logger.info("Speech synthesis completed: {} bytes written to {}", audioData.length, outputFile.getName());
            return outputFile;
            
        } catch (Exception e) {
            logger.error("Speech synthesis failed: {}", e.getMessage());
            throw new SynthesisException("Failed to synthesize speech", e);
        }
    }
    
    /**
     * Synthesize speech with SSML (Speech Synthesis Markup Language) for better control.
     * 
     * @param ssmlText The SSML text to synthesize
     * @param language The target language
     * @param outputFile The output audio file
     * @return The synthesized audio file
     * @throws SynthesisException if synthesis fails
     */
    public File synthesizeSpeechWithSSML(String ssmlText, String language, File outputFile) throws SynthesisException {
        logger.info("Synthesizing SSML speech for language: {} to file: {}", language, outputFile.getName());
        
        try {
            String voiceId = getVoiceId(language);
            
            SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                    .textType(TextType.SSML)
                    .text(ssmlText)
                    .voiceId(voiceId)
                    .outputFormat(OutputFormat.MP3)
                    .engine(Engine.NEURAL)
                    .sampleRate(String.valueOf(sampleRate))
                    .build();
            
            ResponseInputStream<SynthesizeSpeechResponse> response = pollyClient.synthesizeSpeech(request);
            
            // Write audio data to file
            byte[] audioData = response.readAllBytes();
            Files.write(outputFile.toPath(), audioData);
            
            logger.info("SSML speech synthesis completed: {} bytes written to {}", audioData.length, outputFile.getName());
            return outputFile;
            
        } catch (Exception e) {
            logger.error("SSML speech synthesis failed: {}", e.getMessage());
            throw new SynthesisException("Failed to synthesize SSML speech", e);
        }
    }
    
    /**
     * Synthesize speech with emotional tone control.
     * 
     * @param text The text to synthesize
     * @param language The target language
     * @param emotion The emotional tone (e.g., "excited", "sad", "neutral")
     * @param outputFile The output audio file
     * @return The synthesized audio file
     * @throws SynthesisException if synthesis fails
     */
    public File synthesizeSpeechWithEmotion(String text, String language, String emotion, File outputFile) throws SynthesisException {
        logger.info("Synthesizing emotional speech for language: {} with emotion: {}", language, emotion);
        
        // Create SSML with emotional prosody
        String ssmlText = createEmotionalSSML(text, emotion);
        
        return synthesizeSpeechWithSSML(ssmlText, language, outputFile);
    }
    
    /**
     * Synthesize speech with pace control.
     * 
     * @param text The text to synthesize
     * @param language The target language
     * @param pace The speaking pace (slow, medium, fast)
     * @param outputFile The output audio file
     * @return The synthesized audio file
     * @throws SynthesisException if synthesis fails
     */
    public File synthesizeSpeechWithPace(String text, String language, String pace, File outputFile) throws SynthesisException {
        logger.info("Synthesizing speech with pace control for language: {} with pace: {}", language, pace);
        
        // Create SSML with pace control
        String ssmlText = createPaceSSML(text, pace);
        
        return synthesizeSpeechWithSSML(ssmlText, language, outputFile);
    }
    
    /**
     * Get available voices for a language.
     * 
     * @param languageCode The language code
     * @return List of available voices
     * @throws SynthesisException if retrieval fails
     */
    public java.util.List<Voice> getVoicesForLanguage(String languageCode) throws SynthesisException {
        logger.debug("Retrieving voices for language: {}", languageCode);
        
        try {
            DescribeVoicesRequest request = DescribeVoicesRequest.builder()
                    .languageCode(languageCode)
                    .engine(Engine.NEURAL)
                    .build();
            
            DescribeVoicesResponse response = pollyClient.describeVoices(request);
            
            java.util.List<Voice> voices = response.voices();
            logger.debug("Retrieved {} voices for language: {}", voices.size(), languageCode);
            
            return voices;
            
        } catch (PollyException e) {
            logger.error("Failed to retrieve voices for language {}: {}", languageCode, e.getMessage());
            throw new SynthesisException("Failed to retrieve voices", e);
        }
    }
    
    /**
     * Get voice ID for a language.
     * 
     * @param language The language name
     * @return The voice ID
     */
    private String getVoiceId(String language) {
        String voiceId = VOICE_MAPPINGS.get(language.toLowerCase());
        if (voiceId == null) {
            logger.warn("No voice mapping found for language: {}, using default", language);
            return "Joanna"; // Default to English voice
        }
        return voiceId;
    }
    
    /**
     * Create SSML with emotional prosody.
     * 
     * @param text The text to synthesize
     * @param emotion The emotional tone
     * @return The SSML text
     */
    private String createEmotionalSSML(String text, String emotion) {
        String prosodyAttributes = switch (emotion.toLowerCase()) {
            case "excited" -> "rate=\"fast\" pitch=\"high\"";
            case "sad" -> "rate=\"slow\" pitch=\"low\"";
            case "angry" -> "rate=\"fast\" pitch=\"high\" volume=\"loud\"";
            case "whisper" -> "volume=\"x-soft\"";
            default -> ""; // neutral
        };
        
        if (prosodyAttributes.isEmpty()) {
            return String.format("<speak>%s</speak>", text);
        } else {
            return String.format("<speak><prosody %s>%s</prosody></speak>", prosodyAttributes, text);
        }
    }
    
    /**
     * Create SSML with pace control.
     * 
     * @param text The text to synthesize
     * @param pace The speaking pace
     * @return The SSML text
     */
    private String createPaceSSML(String text, String pace) {
        String rate = switch (pace.toLowerCase()) {
            case "slow" -> "slow";
            case "fast" -> "fast";
            default -> "medium"; // medium
        };
        
        return String.format("<speak><prosody rate=\"%s\">%s</prosody></speak>", rate, text);
    }
    
    /**
     * Create SSML with pause control.
     * 
     * @param text The text to synthesize
     * @param pauseSeconds The pause duration in seconds
     * @return The SSML text
     */
    public String createSSMLWithPause(String text, double pauseSeconds) {
        return String.format("<speak>%s<break time=\"%.1fs\"/></speak>", text, pauseSeconds);
    }
    
    /**
     * Validate if a voice is available for a language.
     * 
     * @param language The language to check
     * @return true if voice is available, false otherwise
     */
    public boolean isVoiceAvailable(String language) {
        return VOICE_MAPPINGS.containsKey(language.toLowerCase());
    }
    
    /**
     * Get the duration of synthesized audio in seconds.
     * 
     * @param text The text that was synthesized
     * @param language The language used
     * @return The estimated duration in seconds
     */
    public double estimateDuration(String text, String language) {
        // Rough estimation: average speaking rate is about 150 words per minute
        String[] words = text.split("\\s+");
        double wordsPerSecond = 150.0 / 60.0; // words per second
        return words.length / wordsPerSecond;
    }
    
    /**
     * Custom exception for synthesis errors.
     */
    public static class SynthesisException extends Exception {
        public SynthesisException(String message) {
            super(message);
        }
        
        public SynthesisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 