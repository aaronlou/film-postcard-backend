package world.isnap.filmpostcard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import world.isnap.filmpostcard.entity.Album;
import world.isnap.filmpostcard.entity.Photo;
import world.isnap.filmpostcard.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByUserOrderByCreatedAtDesc(User user);
    List<Photo> findByAlbumOrderByCreatedAtDesc(Album album);
    Long countByUser(User user);
    Long countByAlbum(Album album);
    Optional<Photo> findByUserAndImageUrl(User user, String imageUrl);
}
