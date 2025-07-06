package com.videoservice.service;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Service for video processing operations including audio extraction,
 * format validation, and video assembly using FFmpeg.
 */
@Service
public class VideoProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingService.class);
    
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;
    private final FFmpegExecutor executor;
    
    @Value("${video.supported-formats:mp4,avi,mov,mkv,wmv}")
    private List<String> supportedFormats;
    
    @Value("${video.max-duration:3600}")
    private long maxDurationSeconds;
    
    @Value("${video.quality.video-bitrate:2000000}")
    private int videoBitrate;
    
    @Value("${video.quality.audio-bitrate:128000}")
    private int audioBitrate;
    
    public VideoProcessingService(@Value("${video.ffmpeg.path:C:\\ffmpeg\\bin\\ffmpeg.exe}") String ffmpegPath,
                                 @Value("${video.ffmpeg.ffprobe-path:C:\\ffmpeg\\bin\\ffprobe.exe}") String ffprobePath) throws IOException {
        this.ffmpeg = new FFmpeg(ffmpegPath);
        this.ffprobe = new FFprobe(ffprobePath);
        this.executor = new FFmpegExecutor(ffmpeg, ffprobe);
    }
    
    /**
     * Validate if the video file is supported and meets requirements.
     * 
     * @param videoFile The video file to validate
     * @return VideoInfo containing metadata about the video
     * @throws IOException if validation fails
     */
    public VideoInfo validateVideo(File videoFile) throws IOException {
        logger.info("Validating video file: {}", videoFile.getName());
        
        // Check file format
        String extension = getFileExtension(videoFile.getName()).toLowerCase();
        if (!supportedFormats.contains(extension.substring(1))) {
            throw new IOException("Unsupported video format: " + extension);
        }
        
        // Check if file exists and is readable
        if (!videoFile.exists()) {
            throw new IOException("Video file does not exist: " + videoFile.getAbsolutePath());
        }
        
        if (!videoFile.canRead()) {
            throw new IOException("Cannot read video file: " + videoFile.getAbsolutePath());
        }
        
        // Use direct command execution as fallback for validation
        VideoInfo videoInfo = validateVideoWithCommand(videoFile);
        
        logger.info("Video validation successful: {}x{}, {}s, {}MB", 
                   videoInfo.getWidth(), videoInfo.getHeight(), 
                   videoInfo.getDurationSeconds(), 
                   videoInfo.getFileSizeBytes() / (1024 * 1024));
        
        return videoInfo;
    }
    
    /**
     * Validate video using direct FFprobe command execution
     */
    private VideoInfo validateVideoWithCommand(File videoFile) throws IOException {
        try {
            // Get duration
            ProcessBuilder durationBuilder = new ProcessBuilder(
                "C:\\ffmpeg\\bin\\ffprobe.exe", "-v", "quiet", 
                "-show_entries", "format=duration", 
                "-of", "default=noprint_wrappers=1:nokey=1", 
                videoFile.getAbsolutePath()
            );
            
            Process durationProcess = durationBuilder.start();
            String durationOutput = new String(durationProcess.getInputStream().readAllBytes()).trim();
            int durationExitCode = durationProcess.waitFor();
            
            if (durationExitCode != 0) {
                throw new IOException("Failed to get video duration");
            }
            
            double duration = Double.parseDouble(durationOutput);
            if (duration > maxDurationSeconds) {
                throw new IOException("Video duration exceeds maximum allowed duration: " + 
                                    formatDuration(duration) + " > " + formatDuration(maxDurationSeconds));
            }
            
            // Get video stream info
            ProcessBuilder videoBuilder = new ProcessBuilder(
                "C:\\ffmpeg\\bin\\ffprobe.exe", "-v", "quiet", 
                "-select_streams", "v:0", 
                "-show_entries", "stream=width,height,codec_name,r_frame_rate", 
                "-of", "json", 
                videoFile.getAbsolutePath()
            );
            
            Process videoProcess = videoBuilder.start();
            String videoOutput = new String(videoProcess.getInputStream().readAllBytes());
            int videoExitCode = videoProcess.waitFor();
            
            if (videoExitCode != 0) {
                throw new IOException("Failed to get video stream info");
            }
            
            // Parse JSON output (simplified)
            if (!videoOutput.contains("\"width\"") || !videoOutput.contains("\"height\"")) {
                throw new IOException("No video stream found in file");
            }
            
            // Extract basic info from JSON (simplified parsing)
            int width = extractJsonValue(videoOutput, "width");
            int height = extractJsonValue(videoOutput, "height");
            String codec = extractJsonString(videoOutput, "codec_name");
            String frameRate = extractJsonString(videoOutput, "r_frame_rate");
            
            // Get audio stream info
            ProcessBuilder audioBuilder = new ProcessBuilder(
                "C:\\ffmpeg\\bin\\ffprobe.exe", "-v", "quiet", 
                "-select_streams", "a:0", 
                "-show_entries", "stream=sample_rate,channels,codec_name", 
                "-of", "json", 
                videoFile.getAbsolutePath()
            );
            
            Process audioProcess = audioBuilder.start();
            String audioOutput = new String(audioProcess.getInputStream().readAllBytes());
            int audioExitCode = audioProcess.waitFor();
            
            VideoInfo videoInfo = new VideoInfo();
            videoInfo.setDurationSeconds((long) Math.round(duration));
            videoInfo.setFileSizeBytes(videoFile.length());
            videoInfo.setVideoCodec(codec != null ? codec : "unknown");
            videoInfo.setWidth(width);
            videoInfo.setHeight(height);
            videoInfo.setFrameRate(frameRate != null ? frameRate : "25/1");
            
            if (audioExitCode == 0 && audioOutput.contains("\"sample_rate\"")) {
                int sampleRate = extractJsonValue(audioOutput, "sample_rate");
                int channels = extractJsonValue(audioOutput, "channels");
                String audioCodec = extractJsonString(audioOutput, "codec_name");
                
                videoInfo.setAudioCodec(audioCodec != null ? audioCodec : "unknown");
                videoInfo.setSampleRate(sampleRate);
                videoInfo.setChannels(channels);
            } else {
                videoInfo.setAudioCodec("none");
                videoInfo.setSampleRate(0);
                videoInfo.setChannels(0);
                logger.warn("No audio stream found in file: {}. This may cause issues with translation.", videoFile.getName());
            }
            
            return videoInfo;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Video validation interrupted", e);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid video metadata format", e);
        }
    }
    
    private int extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {
            logger.warn("Failed to extract {} from JSON: {}", key, e.getMessage());
        }
        return 0;
    }
    
    private String extractJsonString(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract {} from JSON: {}", key, e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract audio from video file.
     * 
     * @param videoFile The input video file
     * @param outputAudioFile The output audio file
     * @return The extracted audio file
     * @throws IOException if extraction fails
     */
    public File extractAudio(File videoFile, File outputAudioFile) throws IOException {
        logger.info("Extracting audio from video: {} to {}", videoFile.getName(), outputAudioFile.getName());
        
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoFile.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(outputAudioFile.getAbsolutePath())
                .setFormat("mp3")
                .setAudioCodec("libmp3lame")
                .setAudioSampleRate(16000)
                .setAudioChannels(1) // Mono for better processing
                .done();
        
        try {
            executor.createJob(builder).run();
            logger.info("Audio extraction completed: {}", outputAudioFile.getName());
            return outputAudioFile;
        } catch (Exception e) {
            logger.error("Failed to extract audio from video: {}", e.getMessage());
            throw new IOException("Failed to extract audio from video", e);
        }
    }
    
    /**
     * Split audio into chunks of specified duration.
     * 
     * @param audioFile The input audio file
     * @param chunkDurationSeconds The duration of each chunk in seconds
     * @param outputDirectory The directory to save chunks
     * @return List of chunk files
     * @throws IOException if chunking fails
     */
    public List<File> splitAudioIntoChunks(File audioFile, int chunkDurationSeconds, Path outputDirectory) throws IOException {
        logger.info("Splitting audio into {}s chunks: {}", chunkDurationSeconds, audioFile.getName());
        
        // Get audio duration
        FFmpegProbeResult probeResult = ffprobe.probe(audioFile.getAbsolutePath());
        double totalDuration = probeResult.getFormat().duration;
        
        // Calculate number of chunks
        int numChunks = (int) Math.ceil(totalDuration / chunkDurationSeconds);
        List<File> chunkFiles = new java.util.ArrayList<>();
        
        for (int i = 0; i < numChunks; i++) {
            double startTime = i * (double) chunkDurationSeconds;
            double endTime = Math.min((i + 1) * (double) chunkDurationSeconds, totalDuration);
            
            File chunkFile = outputDirectory.resolve(String.format("chunk_%03d.mp3", i)).toFile();
            
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(audioFile.getAbsolutePath())
                    .overrideOutputFiles(true)
                    .addOutput(chunkFile.getAbsolutePath())
                    .setStartOffset((long) startTime, java.util.concurrent.TimeUnit.SECONDS)
                    .setDuration((long) (endTime - startTime), java.util.concurrent.TimeUnit.SECONDS)
                    .setFormat("mp3")
                    .setAudioCodec("libmp3lame")
                    .done();
            
            try {
                executor.createJob(builder).run();
                chunkFiles.add(chunkFile);
                logger.debug("Created audio chunk {}: {}", i, chunkFile.getName());
            } catch (Exception e) {
                logger.error("Failed to create audio chunk {}: {}", i, e.getMessage());
                throw new IOException("Failed to create audio chunk", e);
            }
        }
        
        logger.info("Audio splitting completed: {} chunks created", chunkFiles.size());
        return chunkFiles;
    }
    
    /**
     * Merge audio chunks back into a single file.
     * 
     * @param audioChunks List of audio chunk files
     * @param outputFile The output merged audio file
     * @return The merged audio file
     * @throws IOException if merging fails
     */
    public File mergeAudioChunks(List<File> audioChunks, File outputFile) throws IOException {
        logger.info("Merging {} audio chunks into: {}", audioChunks.size(), outputFile.getName());
        
        // Create a file list for FFmpeg
        File fileList = createFileList(audioChunks);
        
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(fileList.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(outputFile.getAbsolutePath())
                .setFormat("mp3")
                .setAudioCodec("libmp3lame")
                .done();
        
        try {
            executor.createJob(builder).run();
            logger.info("Audio merging completed: {}", outputFile.getName());
            return outputFile;
        } catch (Exception e) {
            logger.error("Failed to merge audio chunks: {}", e.getMessage());
            throw new IOException("Failed to merge audio chunks", e);
        } finally {
            // Clean up file list
            if (fileList.exists()) {
                fileList.delete();
            }
        }
    }
    
    /**
     * Replace audio in video with new audio file.
     * 
     * @param videoFile The original video file
     * @param audioFile The new audio file
     * @param outputFile The output video file
     * @return The output video file
     * @throws IOException if replacement fails
     */
    public File replaceAudioInVideo(File videoFile, File audioFile, File outputFile) throws IOException {
        logger.info("Replacing audio in video: {} with audio: {}", videoFile.getName(), audioFile.getName());
        
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoFile.getAbsolutePath())
                .addInput(audioFile.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(outputFile.getAbsolutePath())
                .setVideoCodec("copy") // Copy video stream without re-encoding
                .setAudioCodec("aac")
                // .setVideoBitrate(videoBitrate) // Not available in this FFmpeg library version
                .setFormat("mp4")
                .done();
        
        try {
            executor.createJob(builder).run();
            logger.info("Audio replacement completed: {}", outputFile.getName());
            return outputFile;
        } catch (Exception e) {
            logger.error("Failed to replace audio in video: {}", e.getMessage());
            throw new IOException("Failed to replace audio in video", e);
        }
    }
    
    /**
     * Get video thumbnail.
     * 
     * @param videoFile The video file
     * @param outputFile The output thumbnail file
     * @param timeSeconds The time in seconds to extract thumbnail
     * @return The thumbnail file
     * @throws IOException if thumbnail extraction fails
     */
    public File extractThumbnail(File videoFile, File outputFile, double timeSeconds) throws IOException {
        logger.info("Extracting thumbnail from video: {} at {}s", videoFile.getName(), timeSeconds);
        
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoFile.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(outputFile.getAbsolutePath())
                .setStartOffset((long) timeSeconds, java.util.concurrent.TimeUnit.SECONDS)
                .setVideoCodec("mjpeg")
                .setFormat("image2")
                .done();
        
        try {
            executor.createJob(builder).run();
            logger.info("Thumbnail extraction completed: {}", outputFile.getName());
            return outputFile;
        } catch (Exception e) {
            logger.error("Failed to extract thumbnail: {}", e.getMessage());
            throw new IOException("Failed to extract thumbnail", e);
        }
    }
    
    /**
     * Create a file list for FFmpeg concatenation.
     * 
     * @param files List of files to concatenate
     * @return The file list file
     * @throws IOException if file creation fails
     */
    private File createFileList(List<File> files) throws IOException {
        File fileList = File.createTempFile("ffmpeg_filelist_", ".txt");
        
        StringBuilder content = new StringBuilder();
        for (File file : files) {
            content.append("file '").append(file.getAbsolutePath()).append("'\n");
        }
        
        Files.write(fileList.toPath(), content.toString().getBytes());
        return fileList;
    }
    
    /**
     * Get file extension from filename.
     * 
     * @param filename The filename
     * @return The file extension including the dot
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
    
    /**
     * Format duration in seconds to human readable format.
     * 
     * @param seconds Duration in seconds
     * @return Formatted duration string
     */
    private String formatDuration(double seconds) {
        long hours = (long) Math.floor(seconds / 3600);
        long minutes = (long) Math.floor((seconds % 3600) / 60);
        long secs = (long) Math.floor(seconds % 60);
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }
    
    /**
     * Inner class to hold video metadata information.
     */
    public static class VideoInfo {
        private long durationSeconds;
        private long fileSizeBytes;
        private String videoCodec;
        private String audioCodec;
        private int width;
        private int height;
        private String frameRate;
        private int sampleRate;
        private int channels;
        
        // Getters and Setters
        public long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
        
        public long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
        
        public String getVideoCodec() { return videoCodec; }
        public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }
        
        public String getAudioCodec() { return audioCodec; }
        public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }
        
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public String getFrameRate() { return frameRate; }
        public void setFrameRate(String frameRate) { this.frameRate = frameRate; }
        
        public int getSampleRate() { return sampleRate; }
        public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
        
        public int getChannels() { return channels; }
        public void setChannels(int channels) { this.channels = channels; }
    }
} 