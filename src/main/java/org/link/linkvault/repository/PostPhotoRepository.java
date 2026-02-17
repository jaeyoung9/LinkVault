package org.link.linkvault.repository;

import org.link.linkvault.entity.PostPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostPhotoRepository extends JpaRepository<PostPhoto, Long> {

    List<PostPhoto> findByBookmarkId(Long bookmarkId);

    void deleteByBookmarkId(Long bookmarkId);
}
