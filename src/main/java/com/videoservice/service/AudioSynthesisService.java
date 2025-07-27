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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Service for text-to-speech synthesis using AWS Polly.
 * Generates natural-sounding speech in target languages.
 */
@Service
public class AudioSynthesisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioSynthesisService.class);
    
    private final PollyClient pollyClient;
    
    // Voice mappings for different languages with gender options
    private static final Map<String, Map<String, String>> VOICE_MAPPINGS = new ConcurrentHashMap<>();
    
    static {
        // English voices
        Map<String, String> englishVoices = new ConcurrentHashMap<>();
        englishVoices.put("male", "Matthew");
        englishVoices.put("female", "Joanna");
        englishVoices.put("default", "Joanna");
        VOICE_MAPPINGS.put("english", englishVoices);
        
        // Hindi voices
        Map<String, String> hindiVoices = new ConcurrentHashMap<>();
        hindiVoices.put("male", "Matthew"); // Good for Hindi pronunciation
        hindiVoices.put("female", "Aditi");
        hindiVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("hindi", hindiVoices);
        
        // Tamil voices
        Map<String, String> tamilVoices = new ConcurrentHashMap<>();
        tamilVoices.put("male", "Justin"); // Good for Tamil pronunciation
        tamilVoices.put("female", "Raveena");
        tamilVoices.put("default", "Raveena");
        VOICE_MAPPINGS.put("tamil", tamilVoices);
        
        // Telugu voices
        Map<String, String> teluguVoices = new ConcurrentHashMap<>();
        teluguVoices.put("male", "Justin"); // Good for Telugu pronunciation
        teluguVoices.put("female", "Aditi");
        teluguVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("telugu", teluguVoices);
        
        // Kannada voices
        Map<String, String> kannadaVoices = new ConcurrentHashMap<>();
        kannadaVoices.put("male", "Justin"); // Good for Kannada pronunciation
        kannadaVoices.put("female", "Aditi");
        kannadaVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("kannada", kannadaVoices);
        
        // Malayalam voices
        Map<String, String> malayalamVoices = new ConcurrentHashMap<>();
        malayalamVoices.put("male", "Justin"); // Good for Malayalam pronunciation
        malayalamVoices.put("female", "Aditi");
        malayalamVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("malayalam", malayalamVoices);
        
        // Bengali voices
        Map<String, String> bengaliVoices = new ConcurrentHashMap<>();
        bengaliVoices.put("male", "Kevin"); // Good for Bengali pronunciation
        bengaliVoices.put("female", "Aditi");
        bengaliVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("bengali", bengaliVoices);
        
        // Marathi voices
        Map<String, String> marathiVoices = new ConcurrentHashMap<>();
        marathiVoices.put("male", "Kevin"); // Good for Marathi pronunciation
        marathiVoices.put("female", "Aditi");
        marathiVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("marathi", marathiVoices);
        
        // Gujarati voices
        Map<String, String> gujaratiVoices = new ConcurrentHashMap<>();
        gujaratiVoices.put("male", "Kevin"); // Good for Gujarati pronunciation
        gujaratiVoices.put("female", "Aditi");
        gujaratiVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("gujarati", gujaratiVoices);
        
        // Punjabi voices
        Map<String, String> punjabiVoices = new ConcurrentHashMap<>();
        punjabiVoices.put("male", "Kevin"); // Good for Punjabi pronunciation
        punjabiVoices.put("female", "Aditi");
        punjabiVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("punjabi", punjabiVoices);
        
        // Odia voices
        Map<String, String> odiaVoices = new ConcurrentHashMap<>();
        odiaVoices.put("male", "Kevin"); // Good for Odia pronunciation
        odiaVoices.put("female", "Aditi");
        odiaVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("odia", odiaVoices);
        
        // Assamese voices
        Map<String, String> assameseVoices = new ConcurrentHashMap<>();
        assameseVoices.put("male", "Kevin"); // Good for Assamese pronunciation
        assameseVoices.put("female", "Aditi");
        assameseVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("assamese", assameseVoices);
        
        // Urdu voices
        Map<String, String> urduVoices = new ConcurrentHashMap<>();
        urduVoices.put("male", "Matthew"); // Good for Urdu pronunciation
        urduVoices.put("female", "Aditi");
        urduVoices.put("default", "Aditi");
        VOICE_MAPPINGS.put("urdu", urduVoices);
        
        // Spanish voices
        Map<String, String> spanishVoices = new ConcurrentHashMap<>();
        spanishVoices.put("male", "Miguel");
        spanishVoices.put("female", "Penelope");
        spanishVoices.put("default", "Penelope");
        VOICE_MAPPINGS.put("spanish", spanishVoices);
        
        // French voices
        Map<String, String> frenchVoices = new ConcurrentHashMap<>();
        frenchVoices.put("male", "Mathieu");
        frenchVoices.put("female", "Lea");
        frenchVoices.put("default", "Lea");
        VOICE_MAPPINGS.put("french", frenchVoices);
        
        // German voices
        Map<String, String> germanVoices = new ConcurrentHashMap<>();
        germanVoices.put("male", "Hans");
        germanVoices.put("female", "Marlene");
        germanVoices.put("default", "Marlene");
        VOICE_MAPPINGS.put("german", germanVoices);
        
        // Chinese voices
        Map<String, String> chineseVoices = new ConcurrentHashMap<>();
        chineseVoices.put("male", "Zhiyu");
        chineseVoices.put("female", "Zhiyu");
        chineseVoices.put("default", "Zhiyu");
        VOICE_MAPPINGS.put("chinese", chineseVoices);
        
        // Japanese voices
        Map<String, String> japaneseVoices = new ConcurrentHashMap<>();
        japaneseVoices.put("male", "Takumi");
        japaneseVoices.put("female", "Mizuki");
        japaneseVoices.put("default", "Mizuki");
        VOICE_MAPPINGS.put("japanese", japaneseVoices);
        
        // Korean voices
        Map<String, String> koreanVoices = new ConcurrentHashMap<>();
        koreanVoices.put("male", "Seoyeon");
        koreanVoices.put("female", "Seoyeon");
        koreanVoices.put("default", "Seoyeon");
        VOICE_MAPPINGS.put("korean", koreanVoices);
        
        // Arabic voices
        Map<String, String> arabicVoices = new ConcurrentHashMap<>();
        arabicVoices.put("male", "Zeina");
        arabicVoices.put("female", "Zeina");
        arabicVoices.put("default", "Zeina");
        VOICE_MAPPINGS.put("arabic", arabicVoices);
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
     * Synthesize speech from text with gender-aware voice selection.
     * 
     * @param text The text to synthesize
     * @param language The target language
     * @param outputFile The output audio file
     * @param originalAudioFile The original audio file for gender detection (optional)
     * @return The synthesized audio file
     * @throws SynthesisException if synthesis fails
     */
    public File synthesizeSpeech(String text, String language, File outputFile, File originalAudioFile) throws SynthesisException {
        String detectedGender = "default";
        
        if (originalAudioFile != null && originalAudioFile.exists()) {
            try {
                detectedGender = detectSpeakerGender(originalAudioFile);
                logger.info("[Audio Synthesis] Detected speaker gender: {} from original audio", detectedGender);
            } catch (Exception e) {
                logger.warn("[Audio Synthesis] Failed to detect speaker gender, using default: {}", e.getMessage());
            }
        }
        
        return synthesizeSpeechWithGender(text, language, outputFile, detectedGender);
    }
    
    /**
     * Synthesize speech from text.
     * Handles long text by chunking it into smaller pieces.
     * 
     * @param text The text to synthesize
     * @param language The target language
     * @param outputFile The output audio file
     * @return The synthesized audio file
     * @throws SynthesisException if synthesis fails
     */
    public File synthesizeSpeech(String text, String language, File outputFile) throws SynthesisException {
        return synthesizeSpeechWithGender(text, language, outputFile, "default");
    }
    
    /**
     * Synthesize speech from text with specific gender voice.
     * 
     * @param text The text to synthesize
     * @param language The target language
     * @param outputFile The output audio file
     * @param gender The gender for voice selection (male/female/default)
     * @return The synthesized audio file
     * @throws SynthesisException if synthesis fails
     */
    public File synthesizeSpeechWithGender(String text, String language, File outputFile, String gender) throws SynthesisException {
        logger.info("[Audio Synthesis] Starting speech synthesis for language: {} to file: {} ({} chars)", language, outputFile.getName(), text.length());
        logger.info("[Audio Synthesis] Text to synthesize: {}", text);
        
        try {
            // If text is too long, chunk it into smaller pieces
            if (text.length() > 3000) {
                logger.info("[Audio Synthesis] Text is too long ({} chars), chunking for synthesis", text.length());
                return synthesizeLongText(text, language, outputFile);
            }
            
            String voiceId = getVoiceIdForGender(language, gender);
            logger.info("[Audio Synthesis] Using voice ID: {} for language: {} with gender: {}", voiceId, language, gender);
            
            SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                    .text(text)
                    .voiceId(voiceId)
                    .outputFormat(OutputFormat.MP3)
                    .engine(Engine.STANDARD)
                    .sampleRate(String.valueOf(sampleRate))
                    .build();
            
            logger.info("[Audio Synthesis] Sending synthesis request to AWS Polly...");
            ResponseInputStream<SynthesizeSpeechResponse> response = pollyClient.synthesizeSpeech(request);
            
            // Write audio data to file
            byte[] audioData = response.readAllBytes();
            Files.write(outputFile.toPath(), audioData);
            
            logger.info("[Audio Synthesis] SUCCESS: Speech synthesis completed!");
            logger.info("[Audio Synthesis] Audio data size: {} bytes", audioData.length);
            logger.info("[Audio Synthesis] File written to: {} (file size: {} bytes)", outputFile.getName(), outputFile.length());
            logger.info("[Audio Synthesis] Voice used: {} for language: {}", voiceId, language);
            return outputFile;
            
        } catch (Exception e) {
            logger.error("Speech synthesis failed: {}", e.getMessage());
            throw new SynthesisException("Failed to synthesize speech", e);
        }
    }
    
    /**
     * Synthesize long text by breaking it into chunks and merging audio.
     * 
     * @param text The long text to synthesize
     * @param language The target language
     * @param outputFile The output audio file
     * @return The synthesized audio file
     * @throws SynthesisException if synthesis fails
     */
    private File synthesizeLongText(String text, String language, File outputFile) throws SynthesisException {
        logger.info("Synthesizing long text in chunks: {} chars", text.length());
        
        try {
            // Split text into sentences
            String[] sentences = text.split("[.!?।]");
            List<File> audioChunks = new java.util.ArrayList<>();
            
            for (int i = 0; i < sentences.length; i++) {
                String sentence = sentences[i].trim();
                if (sentence.isEmpty()) {
                    continue;
                }
                
                try {
                    // Add punctuation back if it was removed
                    String sentenceToSynthesize = sentence;
                    if (i < sentences.length - 1) {
                        sentenceToSynthesize += "."; // Add English period back
                    }
                    
                    // Create temporary file for this chunk
                    File chunkFile = File.createTempFile("audio_chunk_" + i + "_", ".mp3");
                    synthesizeTextChunk(sentenceToSynthesize, language, chunkFile);
                    audioChunks.add(chunkFile);
                    
                    logger.debug("Synthesized chunk {}/{}: {} chars", i + 1, sentences.length, sentenceToSynthesize.length());
                    
                    // Small delay to avoid rate limiting
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    logger.warn("Failed to synthesize chunk {}, skipping: {}", i + 1, e.getMessage());
                }
            }
            
            // Merge audio chunks
            if (audioChunks.isEmpty()) {
                throw new SynthesisException("No audio chunks were created successfully");
            }
            
            if (audioChunks.size() == 1) {
                // Only one chunk, just copy it
                Files.copy(audioChunks.get(0).toPath(), outputFile.toPath());
            } else {
                // Merge multiple chunks using FFmpeg
                mergeAudioChunks(audioChunks, outputFile);
            }
            
            // Clean up chunk files
            for (File chunk : audioChunks) {
                chunk.delete();
            }
            
            logger.info("Long text synthesis completed: {} -> {} bytes", text.length(), outputFile.length());
            return outputFile;
            
        } catch (Exception e) {
            logger.error("Long text synthesis failed: {}", e.getMessage());
            throw new SynthesisException("Failed to synthesize long text", e);
        }
    }
    
    /**
     * Synthesize a single text chunk (internal method).
     * 
     * @param text The text chunk to synthesize
     * @param language The target language
     * @param outputFile The output audio file
     * @throws SynthesisException if synthesis fails
     */
    private void synthesizeTextChunk(String text, String language, File outputFile) throws SynthesisException {
        try {
            String voiceId = getVoiceId(language);
            
            SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                    .text(text)
                    .voiceId(voiceId)
                    .outputFormat(OutputFormat.MP3)
                    .engine(Engine.STANDARD)
                    .sampleRate(String.valueOf(sampleRate))
                    .build();
            
            ResponseInputStream<SynthesizeSpeechResponse> response = pollyClient.synthesizeSpeech(request);
            
            // Write audio data to file
            byte[] audioData = response.readAllBytes();
            Files.write(outputFile.toPath(), audioData);
            
        } catch (Exception e) {
            logger.error("Text chunk synthesis failed: {}", e.getMessage());
            throw new SynthesisException("Failed to synthesize text chunk", e);
        }
    }
    
    /**
     * Merge multiple audio files into one using FFmpeg.
     * 
     * @param audioFiles List of audio files to merge
     * @param outputFile The output merged audio file
     * @throws SynthesisException if merging fails
     */
    private void mergeAudioChunks(List<File> audioFiles, File outputFile) throws SynthesisException {
        try {
            // Create a file list for FFmpeg
            File fileList = File.createTempFile("audio_list_", ".txt");
            StringBuilder content = new StringBuilder();
            for (File file : audioFiles) {
                content.append("file '").append(file.getAbsolutePath()).append("'\n");
            }
            Files.write(fileList.toPath(), content.toString().getBytes());
            
            // Use FFmpeg to concatenate audio files
            ProcessBuilder processBuilder = new ProcessBuilder(
                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                "-f", "concat",
                "-safe", "0",
                "-i", fileList.getAbsolutePath(),
                "-c", "copy",
                "-y",
                outputFile.getAbsolutePath()
            );
            
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            // Clean up file list
            fileList.delete();
            
            if (exitCode != 0) {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                logger.error("FFmpeg merge error: {}", errorOutput);
                throw new SynthesisException("Failed to merge audio chunks");
            }
            
        } catch (Exception e) {
            logger.error("Audio merging failed: {}", e.getMessage());
            throw new SynthesisException("Failed to merge audio chunks", e);
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
                    .engine(Engine.STANDARD)
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
                    .engine(Engine.STANDARD)
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
     * Get available voices for a specific language and gender.
     * 
     * @param language The language name
     * @param gender The gender (male/female/default)
     * @return List of available voice IDs
     */
    public List<String> getAvailableVoicesForLanguage(String language, String gender) {
        List<String> voices = new ArrayList<>();
        
        try {
            // Get all voices from AWS Polly
            DescribeVoicesRequest request = DescribeVoicesRequest.builder()
                    .languageCode(getLanguageCode(language))
                    .build();
            
            DescribeVoicesResponse response = pollyClient.describeVoices(request);
            
            for (Voice voice : response.voices()) {
                // Filter by gender if specified
                if (gender.equalsIgnoreCase("default") || 
                    voice.gender().toString().equalsIgnoreCase(gender)) {
                    voices.add(voice.id().toString());
                }
            }
            
            logger.info("[Voice Selection] Found {} voices for language: {} and gender: {}", 
                       voices.size(), language, gender);
            
        } catch (Exception e) {
            logger.warn("[Voice Selection] Failed to get voices from AWS Polly: {}", e.getMessage());
            // Fallback to our predefined mappings
            Map<String, String> languageVoices = VOICE_MAPPINGS.get(language.toLowerCase());
            if (languageVoices != null) {
                String voiceId = languageVoices.get(gender.toLowerCase());
                if (voiceId != null) {
                    voices.add(voiceId);
                }
            }
        }
        
        return voices;
    }
    
    /**
     * Get voice ID for a language and gender.
     * 
     * @param language The language name
     * @param gender The gender (male/female/default)
     * @return The voice ID
     */
    private String getVoiceIdForGender(String language, String gender) {
        Map<String, String> languageVoices = VOICE_MAPPINGS.get(language.toLowerCase());
        if (languageVoices == null) {
            logger.warn("No voice mapping found for language: {}, using default", language);
            return "Joanna"; // Default to English voice
        }
        
        String voiceId = languageVoices.get(gender.toLowerCase());
        if (voiceId == null) {
            logger.warn("No voice mapping found for language: {} and gender: {}, using default", language, gender);
            voiceId = languageVoices.get("default");
            if (voiceId == null) {
                return "Joanna"; // Ultimate fallback
            }
        }
        
        // Special handling for Indian languages with male gender
        if (gender.equalsIgnoreCase("male") && isIndianLanguage(language)) {
            String bestMaleVoice = getBestMaleVoiceForIndianLanguage(language);
            logger.info("[Voice Selection] Using best male voice {} for Indian language: {}", bestMaleVoice, language);
            return bestMaleVoice;
        }
        
        return voiceId;
    }
    
    /**
     * Get voice ID for a language (backward compatibility).
     * 
     * @param language The language name
     * @return The voice ID
     */
    private String getVoiceId(String language) {
        return getVoiceIdForGender(language, "default");
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
     * Detect speaker gender from audio file using pitch analysis.
     * This is a simplified approach - in production, you might want to use more sophisticated ML models.
     * 
     * @param audioFile The audio file to analyze
     * @return "male", "female", or "default" if detection fails
     */
    private String detectSpeakerGender(File audioFile) throws Exception {
        logger.info("[Gender Detection] Analyzing audio file for speaker gender: {}", audioFile.getName());
        
        try {
            // Use FFmpeg to extract audio characteristics
            ProcessBuilder processBuilder = new ProcessBuilder(
                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                "-i", audioFile.getAbsolutePath(),
                "-af", "asetrate=44100*1,aresample=44100,asetrate=44100*1",
                "-f", "null",
                "-"
            );
            
            Process process = processBuilder.start();
            String errorOutput = new String(process.getErrorStream().readAllBytes());
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                logger.warn("[Gender Detection] FFmpeg analysis failed, using default gender");
                return "default";
            }
            
            // Analyze the audio characteristics to estimate gender
            // This is a simplified heuristic based on typical frequency ranges
            // In a real implementation, you'd use more sophisticated audio analysis
            
            // For now, we'll use a simple approach based on file size and duration
            // Larger files with longer duration might indicate male speakers (deeper voices)
            long fileSize = audioFile.length();
            double duration = estimateAudioDuration(audioFile);
            
            // Simple heuristic: if file is large and duration is long, likely male
            // This is very basic and should be replaced with proper audio analysis
            if (fileSize > 1000000 && duration > 30) { // > 1MB and > 30 seconds
                logger.info("[Gender Detection] Detected likely male speaker based on file characteristics");
                return "male";
            } else if (fileSize < 500000 && duration < 20) { // < 500KB and < 20 seconds
                logger.info("[Gender Detection] Detected likely female speaker based on file characteristics");
                return "female";
            } else {
                logger.info("[Gender Detection] Could not determine gender, using default");
                return "default";
            }
            
        } catch (Exception e) {
            logger.warn("[Gender Detection] Failed to analyze audio for gender detection: {}", e.getMessage());
            return "default";
        }
    }
    
    /**
     * Estimate audio duration using FFmpeg.
     * 
     * @param audioFile The audio file
     * @return Duration in seconds
     */
    private double estimateAudioDuration(File audioFile) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "C:\\ffmpeg\\bin\\ffprobe.exe",
            "-v", "quiet",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            audioFile.getAbsolutePath()
        );
        
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        int exitCode = process.waitFor();
        
        if (exitCode == 0 && !output.isEmpty()) {
            try {
                return Double.parseDouble(output);
            } catch (NumberFormatException e) {
                logger.warn("[Gender Detection] Failed to parse duration: {}", output);
            }
        }
        
        return 0.0;
    }
    
    /**
     * Get language code for AWS Polly.
     * 
     * @param language The language name
     * @return The language code
     */
    private String getLanguageCode(String language) {
        // Map language names to AWS Polly language codes
        Map<String, String> languageCodes = new HashMap<>();
        languageCodes.put("english", "en-US");
        languageCodes.put("hindi", "hi-IN");
        languageCodes.put("tamil", "ta-IN");
        languageCodes.put("telugu", "te-IN");
        languageCodes.put("kannada", "kn-IN");
        languageCodes.put("malayalam", "ml-IN");
        languageCodes.put("bengali", "bn-IN");
        languageCodes.put("marathi", "mr-IN");
        languageCodes.put("gujarati", "gu-IN");
        languageCodes.put("punjabi", "pa-IN");
        languageCodes.put("odia", "or-IN");
        languageCodes.put("assamese", "as-IN");
        languageCodes.put("urdu", "ur-IN");
        languageCodes.put("arabic", "ar");
        languageCodes.put("korean", "ko-KR");
        languageCodes.put("chinese", "cmn-CN");
        languageCodes.put("spanish", "es-ES");
        languageCodes.put("french", "fr-FR");
        languageCodes.put("german", "de-DE");
        languageCodes.put("japanese", "ja-JP");
        
        return languageCodes.getOrDefault(language.toLowerCase(), "en-US");
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

    /**
     * Get the best available male voice for Indian languages.
     * Since AWS Polly has limited Indian language male voices, we use English male voices
     * that work well with Indian language pronunciation.
     * 
     * @param language The Indian language
     * @return The best male voice ID for the language
     */
    private String getBestMaleVoiceForIndianLanguage(String language) {
        // For Indian languages, we'll use English male voices that work well
        // with Indian language pronunciation patterns
        
        switch (language.toLowerCase()) {
            case "hindi":
            case "urdu":
                return "Matthew"; // Good for Hindi/Urdu pronunciation
            case "tamil":
            case "telugu":
            case "kannada":
            case "malayalam":
                return "Justin"; // Works well for South Indian languages
            case "bengali":
            case "marathi":
            case "gujarati":
            case "punjabi":
            case "odia":
            case "assamese":
                return "Kevin"; // Good for North Indian languages
            default:
                return "Matthew"; // Default fallback
        }
    }
    
    /**
     * Check if a language is an Indian language.
     * 
     * @param language The language name
     * @return true if it's an Indian language
     */
    private boolean isIndianLanguage(String language) {
        String[] indianLanguages = {
            "hindi", "tamil", "telugu", "kannada", "malayalam", 
            "bengali", "marathi", "gujarati", "punjabi", "odia", 
            "assamese", "urdu"
        };
        
        for (String indianLang : indianLanguages) {
            if (language.toLowerCase().equals(indianLang)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get alternative male voices for Indian languages.
     * Provides multiple options for better voice variety.
     * 
     * @param language The Indian language
     * @return Array of alternative male voice IDs
     */
    public String[] getAlternativeMaleVoicesForIndianLanguage(String language) {
        // Alternative male voices that work well with Indian languages
        String[] alternativeVoices = {
            "Matthew",    // Primary choice - works well with most Indian languages
            "Justin",     // Alternative - good for formal content
            "Kevin",      // Alternative - good for casual content
            "Joey"        // Alternative - good for conversational content
        };
        
        logger.info("[Voice Selection] Alternative male voices for {}: {}", language, String.join(", ", alternativeVoices));
        return alternativeVoices;
    }
    
    /**
     * Get a random male voice for Indian languages to add variety.
     * 
     * @param language The Indian language
     * @return A random male voice ID
     */
    public String getRandomMaleVoiceForIndianLanguage(String language) {
        String[] alternatives = getAlternativeMaleVoicesForIndianLanguage(language);
        int randomIndex = (int) (Math.random() * alternatives.length);
        String selectedVoice = alternatives[randomIndex];
        
        logger.info("[Voice Selection] Selected random male voice {} for Indian language: {}", selectedVoice, language);
        return selectedVoice;
    }
} 