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
        
        // Get video metadata
        FFmpegProbeResult probeResult = ffprobe.probe(videoFile.getAbsolutePath());
        
        // Find video and audio streams
        FFmpegStream videoStream = null;
        FFmpegStream audioStream = null;
        
        for (FFmpegStream stream : probeResult.getStreams()) {
            if ("video".equals(stream.codec_type)) {
                videoStream = stream;
            } else if ("audio".equals(stream.codec_type)) {
                audioStream = stream;
            }
        }
        
        if (videoStream == null) {
            throw new IOException("No video stream found in file");
        }
        
        if (audioStream == null) {
            throw new IOException("No audio stream found in file");
        }
        
        // Check duration
        double duration = probeResult.getFormat().duration;
        if (duration > maxDurationSeconds) {
            throw new IOException("Video duration exceeds maximum allowed duration: " + 
                                formatDuration(duration) + " > " + formatDuration(maxDurationSeconds));
        }
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setDurationSeconds((long) Math.round(duration));
        videoInfo.setFileSizeBytes(videoFile.length());
        videoInfo.setVideoCodec(videoStream.codec_name);
        videoInfo.setAudioCodec(audioStream.codec_name);
        videoInfo.setWidth(videoStream.width);
        videoInfo.setHeight(videoStream.height);
        videoInfo.setFrameRate(videoStream.r_frame_rate.toString());
        videoInfo.setSampleRate(audioStream.sample_rate);
        videoInfo.setChannels(audioStream.channels);
        
        logger.info("Video validation successful: {}x{}, {}s, {}MB", 
                   videoInfo.getWidth(), videoInfo.getHeight(), 
                   videoInfo.getDurationSeconds(), 
                   videoInfo.getFileSizeBytes() / (1024 * 1024));
        
        return videoInfo;
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