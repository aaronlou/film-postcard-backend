package world.isnap.filmpostcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.isnap.filmpostcard.dto.DownloadRequest;
import world.isnap.filmpostcard.dto.DownloadResponse;
import world.isnap.filmpostcard.entity.Download;
import world.isnap.filmpostcard.repository.DownloadRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DownloadService {
    
    private final DownloadRepository downloadRepository;
    
    @Transactional
    public DownloadResponse recordDownload(DownloadRequest request) {
        LocalDateTime downloadTime = LocalDateTime.now();
        
        // Parse timestamp if provided
        if (request.getTimestamp() != null && !request.getTimestamp().isEmpty()) {
            try {
                downloadTime = LocalDateTime.parse(request.getTimestamp(), 
                    DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                log.warn("Failed to parse timestamp: {}, using current time", request.getTimestamp());
            }
        }
        
        Download download = Download.builder()
                .imageId(request.getImageId())
                .templateType(request.getTemplateType())
                .textContent(request.getText())
                .qrUrl(request.getQrUrl())
                .downloadTimestamp(downloadTime)
                .build();
        
        Download saved = downloadRepository.save(download);
        log.info("Download recorded with ID: {}", saved.getId());
        
        return toResponse(saved);
    }
    
    public List<DownloadResponse> getAllDownloads() {
        return downloadRepository.findAllByOrderByDownloadTimestampDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    public List<DownloadResponse> getDownloadsByImageId(String imageId) {
        return downloadRepository.findByImageId(imageId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    private DownloadResponse toResponse(Download download) {
        return DownloadResponse.builder()
                .id(download.getId())
                .imageId(download.getImageId())
                .templateType(download.getTemplateType())
                .downloadTimestamp(download.getDownloadTimestamp())
                .createdAt(download.getCreatedAt())
                .build();
    }
}
