package world.isnap.filmpostcard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import world.isnap.filmpostcard.entity.Photo;
import world.isnap.filmpostcard.entity.User;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByUserOrderByCreatedAtDesc(User user);
    Long countByUser(User user);
}
