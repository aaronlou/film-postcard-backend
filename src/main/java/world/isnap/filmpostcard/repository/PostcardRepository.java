package world.isnap.filmpostcard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import world.isnap.filmpostcard.entity.Postcard;

import java.util.List;

@Repository
public interface PostcardRepository extends JpaRepository<Postcard, Long> {
    
    List<Postcard> findAllByOrderByCreatedAtDesc();
}
