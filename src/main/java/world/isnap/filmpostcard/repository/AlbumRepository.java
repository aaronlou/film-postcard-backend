package world.isnap.filmpostcard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import world.isnap.filmpostcard.entity.Album;
import world.isnap.filmpostcard.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByUserOrderByCreatedAtDesc(User user);
    Optional<Album> findByIdAndUser(Long id, User user);
    Long countByUser(User user);
}
