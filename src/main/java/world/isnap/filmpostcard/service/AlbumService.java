package world.isnap.filmpostcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.isnap.filmpostcard.dto.AlbumRequest;
import world.isnap.filmpostcard.dto.AlbumResponse;
import world.isnap.filmpostcard.entity.Album;
import world.isnap.filmpostcard.entity.Photo;
import world.isnap.filmpostcard.entity.User;
import world.isnap.filmpostcard.repository.AlbumRepository;
import world.isnap.filmpostcard.repository.PhotoRepository;
import world.isnap.filmpostcard.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlbumService {
    
    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    
    @Transactional
    public AlbumResponse createAlbum(String username, AlbumRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Album name is required");
        }
        
        Album album = Album.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .coverPhoto(request.getCoverPhoto())
                .build();
        
        Album savedAlbum = albumRepository.save(album);
        log.info("Album created: {} by user: {}", savedAlbum.getId(), username);
        
        return toAlbumResponse(savedAlbum);
    }
    
    @Transactional(readOnly = true)
    public List<AlbumResponse> getUserAlbums(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        List<Album> albums = albumRepository.findByUserOrderByCreatedAtDesc(user);
        
        return albums.stream()
                .map(this::toAlbumResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public AlbumResponse updateAlbum(String username, String albumId, AlbumRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        Long albumIdLong = parseAlbumId(albumId);
        
        Album album = albumRepository.findByIdAndUser(albumIdLong, user)
                .orElseThrow(() -> new RuntimeException("Album not found or not owned by user"));
        
        // Update fields if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            album.setName(request.getName());
        }
        if (request.getDescription() != null) {
            album.setDescription(request.getDescription());
        }
        if (request.getCoverPhoto() != null) {
            album.setCoverPhoto(request.getCoverPhoto());
        }
        
        Album updatedAlbum = albumRepository.save(album);
        log.info("Album updated: {} by user: {}", albumId, username);
        
        return toAlbumResponse(updatedAlbum);
    }
    
    @Transactional
    public void deleteAlbum(String username, String albumId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        Long albumIdLong = parseAlbumId(albumId);
        
        Album album = albumRepository.findByIdAndUser(albumIdLong, user)
                .orElseThrow(() -> new RuntimeException("Album not found or not owned by user"));
        
        // Remove album reference from all photos in this album
        List<Photo> photosInAlbum = photoRepository.findByAlbumOrderByCreatedAtDesc(album);
        for (Photo photo : photosInAlbum) {
            photo.setAlbum(null);
        }
        photoRepository.saveAll(photosInAlbum);
        
        // Delete the album
        albumRepository.delete(album);
        log.info("Album deleted: {} by user: {}, {} photos became uncategorized", 
                albumId, username, photosInAlbum.size());
    }
    
    private AlbumResponse toAlbumResponse(Album album) {
        // Count photos in this album
        Long photoCount = photoRepository.countByAlbum(album);
        
        return AlbumResponse.builder()
                .id(String.valueOf(album.getId()))
                .name(album.getName())
                .description(album.getDescription())
                .coverPhoto(album.getCoverPhoto())
                .photoCount(photoCount.intValue())
                .createdAt(album.getCreatedAt().toString())
                .updatedAt(album.getUpdatedAt().toString())
                .build();
    }
    
    private Long parseAlbumId(String albumId) {
        try {
            return Long.parseLong(albumId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid album ID: " + albumId);
        }
    }
}
