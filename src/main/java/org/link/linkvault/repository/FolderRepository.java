package org.link.linkvault.repository;

import org.link.linkvault.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.children WHERE f.parent IS NULL ORDER BY f.displayOrder")
    List<Folder> findRootFolders();

    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.children WHERE f.parent IS NULL AND f.user.id = :userId ORDER BY f.displayOrder")
    List<Folder> findRootFoldersByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.children WHERE f.id = :id")
    Optional<Folder> findByIdWithChildren(Long id);

    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.children WHERE f.id = :id AND f.user.id = :userId")
    Optional<Folder> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<Folder> findByParentIdOrderByDisplayOrderAsc(Long parentId);

    List<Folder> findByNameContainingIgnoreCaseAndUserId(String name, Long userId);

    List<Folder> findByNameContainingIgnoreCase(String name);

    void deleteByUserId(Long userId);
}
