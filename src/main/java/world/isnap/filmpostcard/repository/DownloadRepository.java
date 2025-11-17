package world.isnap.filmpostcard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import world.isnap.filmpostcard.entity.Download;

import java.util.List;

@Repository
public interface DownloadRepository extends JpaRepository<Download, Long> {
    List<Download> findAllByOrderByDownloadTimestampDesc();
    List<Download> findByImageId(String imageId);
}
