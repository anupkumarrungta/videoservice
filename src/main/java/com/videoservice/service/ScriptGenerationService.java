package com.videoservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating word documents with source and target language scripts.
 * Creates side-by-side comparison documents for easy reading and reference.
 */
@Service
public class ScriptGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScriptGenerationService.class);
    
    /**
     * Generate a word document with source and target language scripts side by side.
     * 
     * @param sourceScripts List of source language script chunks
     * @param targetScripts List of target language script chunks
     * @param sourceLanguage The source language name
     * @param targetLanguage The target language name
     * @param outputPath The path where to save the document
     * @return The generated file
     * @throws IOException if file creation fails
     */
    public File generateScriptDocument(List<String> sourceScripts, List<String> targetScripts, 
                                     String sourceLanguage, String targetLanguage, Path outputPath) throws IOException {
        
        logger.info("[Script Generation] Generating script document for {} to {} translation", sourceLanguage, targetLanguage);
        logger.info("[Script Generation] Source scripts: {} chunks", sourceScripts.size());
        logger.info("[Script Generation] Target scripts: {} chunks", targetScripts.size());
        
        // Create the output file
        String fileName = String.format("script_%s_to_%s_%s.txt", 
                                       sourceLanguage.toLowerCase(), 
                                       targetLanguage.toLowerCase(),
                                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        
        File scriptFile = outputPath.resolve(fileName).toFile();
        
        try (FileWriter writer = new FileWriter(scriptFile)) {
            // Write document header
            writeDocumentHeader(writer, sourceLanguage, targetLanguage);
            
            // Write scripts side by side
            writeScriptsSideBySide(writer, sourceScripts, targetScripts, sourceLanguage, targetLanguage);
            
            // Write document footer
            writeDocumentFooter(writer);
            
        } catch (IOException e) {
            logger.error("[Script Generation] Failed to generate script document: {}", e.getMessage());
            throw e;
        }
        
        logger.info("[Script Generation] Script document generated successfully: {} ({} bytes)", 
                   scriptFile.getName(), scriptFile.length());
        
        return scriptFile;
    }
    
    /**
     * Write the document header with metadata.
     */
    private void writeDocumentHeader(FileWriter writer, String sourceLanguage, String targetLanguage) throws IOException {
        writer.write("=".repeat(80) + "\n");
        writer.write("VIDEO TRANSLATION SCRIPT\n");
        writer.write("=".repeat(80) + "\n");
        writer.write("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
        writer.write("Source Language: " + sourceLanguage + "\n");
        writer.write("Target Language: " + targetLanguage + "\n");
        writer.write("=".repeat(80) + "\n\n");
        
        // Write column headers
        writer.write(String.format("%-65s | %-65s\n", 
                                 "SOURCE (" + sourceLanguage.toUpperCase() + ")", 
                                 "TARGET (" + targetLanguage.toUpperCase() + ")"));
        writer.write("-".repeat(65) + "+" + "-".repeat(65) + "\n\n");
    }
    
    /**
     * Write scripts side by side in a readable format.
     */
    private void writeScriptsSideBySide(FileWriter writer, List<String> sourceScripts, 
                                      List<String> targetScripts, String sourceLanguage, String targetLanguage) throws IOException {
        
        int maxChunks = Math.max(sourceScripts.size(), targetScripts.size());
        
        for (int i = 0; i < maxChunks; i++) {
            // Get source script (or placeholder if not available)
            String sourceScript = (i < sourceScripts.size()) ? sourceScripts.get(i) : "[No source script available]";
            String targetScript = (i < targetScripts.size()) ? targetScripts.get(i) : "[No target script available]";
            
            // Clean and format the scripts
            sourceScript = cleanAndFormatScript(sourceScript);
            targetScript = cleanAndFormatScript(targetScript);
            
            // Write chunk header
            writer.write(String.format("CHUNK %d:\n", i + 1));
            writer.write("-".repeat(65) + "+" + "-".repeat(65) + "\n");
            
            // Write scripts side by side
            writeSideBySideText(writer, sourceScript, targetScript);
            
            // Add spacing between chunks
            writer.write("\n" + "=".repeat(80) + "\n\n");
        }
    }
    
    /**
     * Write text side by side with proper formatting.
     */
    private void writeSideBySideText(FileWriter writer, String sourceText, String targetText) throws IOException {
        // Split texts into lines for better formatting
        String[] sourceLines = sourceText.split("\n");
        String[] targetLines = targetText.split("\n");
        
        int maxLines = Math.max(sourceLines.length, targetLines.length);
        
        for (int i = 0; i < maxLines; i++) {
            String sourceLine = (i < sourceLines.length) ? sourceLines[i] : "";
            String targetLine = (i < targetLines.length) ? targetLines[i] : "";
            
            // Use longer line width to avoid truncation
            sourceLine = truncateLine(sourceLine, 60);
            targetLine = truncateLine(targetLine, 60);
            
            // Write the line with wider columns
            writer.write(String.format("%-65s | %-65s\n", sourceLine, targetLine));
        }
    }
    
    /**
     * Clean and format script text for better readability.
     */
    private String cleanAndFormatScript(String script) {
        if (script == null || script.trim().isEmpty()) {
            return "[Empty script]";
        }
        
        // Remove extra whitespace and normalize line breaks
        String cleaned = script.trim()
                              .replaceAll("\\s+", " ")
                              .replaceAll("\\n\\s*\\n", "\n")
                              .replaceAll("\\n+", "\n");
        
        // Add proper sentence endings if missing
        if (!cleaned.endsWith(".") && !cleaned.endsWith("!") && !cleaned.endsWith("?") && !cleaned.endsWith("।")) {
            cleaned += ".";
        }
        
        return cleaned;
    }
    
    /**
     * Truncate a line to fit in the specified width.
     */
    private String truncateLine(String line, int maxWidth) {
        if (line == null) return "";
        if (line.length() <= maxWidth) return line;
        
        // Try to break at word boundaries
        String truncated = line.substring(0, maxWidth - 3) + "...";
        
        // Find the last space before the truncation point
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > maxWidth * 0.6) { // More lenient word boundary check
            truncated = line.substring(0, lastSpace) + "...";
        }
        
        // If the line is very long, try to preserve more content by using multiple lines
        if (line.length() > maxWidth * 2) {
            // Split into multiple lines instead of truncating
            StringBuilder multiLine = new StringBuilder();
            String remaining = line;
            while (remaining.length() > maxWidth) {
                int breakPoint = maxWidth;
                // Try to break at word boundary
                int lastSpaceInRemaining = remaining.lastIndexOf(' ', maxWidth);
                if (lastSpaceInRemaining > maxWidth * 0.5) {
                    breakPoint = lastSpaceInRemaining;
                }
                multiLine.append(remaining.substring(0, breakPoint)).append("\n");
                remaining = remaining.substring(breakPoint).trim();
            }
            if (!remaining.isEmpty()) {
                multiLine.append(remaining);
            }
            return multiLine.toString();
        }
        
        return truncated;
    }
    
    /**
     * Write the document footer.
     */
    private void writeDocumentFooter(FileWriter writer) throws IOException {
        writer.write("\n" + "=".repeat(80) + "\n");
        writer.write("END OF SCRIPT\n");
        writer.write("=".repeat(80) + "\n");
        writer.write("Generated by Video Translation Service\n");
        writer.write("Total chunks processed: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
    }
    
    /**
     * Generate a simple text version of the script for quick reference.
     * 
     * @param sourceScripts List of source language script chunks
     * @param targetScripts List of target language script chunks
     * @param sourceLanguage The source language name
     * @param targetLanguage The target language name
     * @param outputPath The path where to save the document
     * @return The generated file
     * @throws IOException if file creation fails
     */
    public File generateSimpleScript(List<String> sourceScripts, List<String> targetScripts, 
                                   String sourceLanguage, String targetLanguage, Path outputPath) throws IOException {
        
        logger.info("[Script Generation] Generating simple script for {} to {} translation", sourceLanguage, targetLanguage);
        
        // Create the output file
        String fileName = String.format("simple_script_%s_to_%s_%s.txt", 
                                       sourceLanguage.toLowerCase(), 
                                       targetLanguage.toLowerCase(),
                                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        
        File scriptFile = outputPath.resolve(fileName).toFile();
        
        try (FileWriter writer = new FileWriter(scriptFile)) {
            // Write header
            writer.write("VIDEO TRANSLATION SCRIPT\n");
            writer.write("Source: " + sourceLanguage + " | Target: " + targetLanguage + "\n");
            writer.write("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("=".repeat(60) + "\n\n");
            
            // Write scripts in simple format
            int maxChunks = Math.max(sourceScripts.size(), targetScripts.size());
            
            for (int i = 0; i < maxChunks; i++) {
                writer.write("CHUNK " + (i + 1) + ":\n");
                writer.write("-".repeat(30) + "\n");
                
                // Source script
                String sourceScript = (i < sourceScripts.size()) ? sourceScripts.get(i) : "[No source script]";
                writer.write("SOURCE (" + sourceLanguage + "):\n");
                writer.write(cleanAndFormatScript(sourceScript) + "\n\n");
                
                // Target script
                String targetScript = (i < targetScripts.size()) ? targetScripts.get(i) : "[No target script]";
                writer.write("TARGET (" + targetLanguage + "):\n");
                writer.write(cleanAndFormatScript(targetScript) + "\n\n");
                
                writer.write("=".repeat(30) + "\n\n");
            }
            
        } catch (IOException e) {
            logger.error("[Script Generation] Failed to generate simple script: {}", e.getMessage());
            throw e;
        }
        
        logger.info("[Script Generation] Simple script generated: {} ({} bytes)", 
                   scriptFile.getName(), scriptFile.length());
        
        return scriptFile;
    }
} 