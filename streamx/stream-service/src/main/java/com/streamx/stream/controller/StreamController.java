package com.streamx.stream.controller;

import com.streamx.stream.service.StreamService;
import io.minio.StatObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
@Tag(name = "Stream", description = "Video upload and streaming")
public class StreamController {

    private final StreamService streamService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a video file")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId) {

        String key = streamService.uploadVideo(file);
        return ResponseEntity.ok(Map.of(
                "videoKey", key,
                "uploadedBy", userId,
                "message", "Upload successful"
        ));
    }

    @GetMapping("/{key}")
    @Operation(summary = "Stream video with byte-range support")
    public ResponseEntity<InputStreamResource> stream(
            @PathVariable String key,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            @RequestHeader("X-User-Id") String userId) {

        StatObjectResponse info = streamService.getVideoInfo(key);
        long fileSize = info.size();

        streamService.publishWatchedEvent(userId, key);

        if (rangeHeader == null) {
            InputStream stream = streamService.getVideoStream(key);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(new InputStreamResource(stream));
        }

        String[] ranges = rangeHeader.replace("bytes=", "").split("-");
        long start = Long.parseLong(ranges[0]);
        long end = ranges.length > 1 && !ranges[1].isEmpty()
                ? Long.parseLong(ranges[1])
                : fileSize - 1;

        if (end >= fileSize) end = fileSize - 1;

        long contentLength = end - start + 1;
        InputStream rangeStream = streamService.getVideoRange(key, start, end);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new InputStreamResource(rangeStream));
    }
    @GetMapping("/verify/{movieId}")
    @Operation(summary = "Verify movie exists via Catalog Service (Feign + Circuit Breaker)")
    public ResponseEntity<Map<String, Object>> verifyMovie(@PathVariable String movieId) {
        return ResponseEntity.ok(streamService.verifyMovie(movieId));
    }
    @GetMapping("/url/{key}")
    @Operation(summary = "Get presigned URL for video")
    public ResponseEntity<Map<String, String>> getUrl(@PathVariable String key) {
        String url = streamService.getPresignedUrl(key);
        return ResponseEntity.ok(Map.of("url", url));
    }
}