package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.FolderMoveDto;
import org.link.linkvault.dto.FolderRequestDto;
import org.link.linkvault.dto.FolderResponseDto;
import org.link.linkvault.entity.Folder;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderService {

    private final FolderRepository folderRepository;

    private boolean isAdmin(User user) {
        return user.getRole() == Role.SUPER_ADMIN
                || user.getRole() == Role.COMMUNITY_ADMIN
                || user.getRole() == Role.MODERATOR;
    }

    public List<FolderResponseDto> findRootFolders(User currentUser) {
        if (isAdmin(currentUser)) {
            return folderRepository.findRootFolders().stream()
                    .map(FolderResponseDto::from)
                    .collect(Collectors.toList());
        }
        return folderRepository.findRootFoldersByUserId(currentUser.getId()).stream()
                .map(FolderResponseDto::from)
                .collect(Collectors.toList());
    }

    public FolderResponseDto findById(User currentUser, Long id) {
        if (isAdmin(currentUser)) {
            Folder folder = folderRepository.findByIdWithChildren(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + id));
            return FolderResponseDto.from(folder);
        }
        Folder folder = folderRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + id));
        return FolderResponseDto.from(folder);
    }

    public List<FolderResponseDto> findAll(User currentUser) {
        if (isAdmin(currentUser)) {
            return folderRepository.findAll().stream()
                    .map(FolderResponseDto::from)
                    .collect(Collectors.toList());
        }
        return folderRepository.findRootFoldersByUserId(currentUser.getId()).stream()
                .map(FolderResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public FolderResponseDto create(User currentUser, FolderRequestDto requestDto) {
        Folder folder = new Folder(requestDto.getName());
        folder.setUser(currentUser);

        if (requestDto.getParentId() != null) {
            Folder parent = folderRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder not found with id: " + requestDto.getParentId()));
            // Verify ownership of parent folder
            if (!isAdmin(currentUser) && (parent.getUser() == null || !parent.getUser().getId().equals(currentUser.getId()))) {
                throw new SecurityException("Access denied");
            }
            folder.setParent(parent);
            folder.setDisplayOrder(parent.getChildren().size());
        } else {
            List<Folder> roots = isAdmin(currentUser)
                    ? folderRepository.findRootFolders()
                    : folderRepository.findRootFoldersByUserId(currentUser.getId());
            folder.setDisplayOrder(roots.size());
        }

        Folder saved = folderRepository.save(folder);
        return FolderResponseDto.from(saved);
    }

    @Transactional
    public FolderResponseDto update(User currentUser, Long id, FolderRequestDto requestDto) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + id));

        if (!isAdmin(currentUser) && (folder.getUser() == null || !folder.getUser().getId().equals(currentUser.getId()))) {
            throw new SecurityException("Access denied");
        }

        folder.updateName(requestDto.getName());
        return FolderResponseDto.from(folder);
    }

    @Transactional
    public void delete(User currentUser, Long id) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + id));

        if (!isAdmin(currentUser) && (folder.getUser() == null || !folder.getUser().getId().equals(currentUser.getId()))) {
            throw new SecurityException("Access denied");
        }

        folderRepository.delete(folder);
    }

    @Transactional
    public FolderResponseDto move(User currentUser, Long id, FolderMoveDto moveDto) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + id));

        if (!isAdmin(currentUser) && (folder.getUser() == null || !folder.getUser().getId().equals(currentUser.getId()))) {
            throw new SecurityException("Access denied");
        }

        if (moveDto.getTargetFolderId() != null) {
            if (moveDto.getTargetFolderId().equals(id)) {
                throw new IllegalArgumentException("Cannot move folder into itself");
            }
            Folder target = folderRepository.findById(moveDto.getTargetFolderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target folder not found"));
            // Verify ownership of target folder
            if (!isAdmin(currentUser) && (target.getUser() == null || !target.getUser().getId().equals(currentUser.getId()))) {
                throw new SecurityException("Access denied");
            }
            // Prevent moving into own descendant
            Folder check = target;
            while (check != null) {
                if (check.getId().equals(id)) {
                    throw new IllegalArgumentException("Cannot move folder into its own descendant");
                }
                check = check.getParent();
            }
            folder.setParent(target);
        } else {
            folder.setParent(null);
        }
        folder.setDisplayOrder(moveDto.getDisplayOrder());

        return FolderResponseDto.from(folder);
    }

    public List<FolderResponseDto> searchByName(User user, String query) {
        List<Folder> folders;
        if (isAdmin(user)) {
            folders = folderRepository.findByNameContainingIgnoreCase(query);
        } else {
            folders = folderRepository.findByNameContainingIgnoreCaseAndUserId(query, user.getId());
        }
        return folders.stream()
                .map(FolderResponseDto::from)
                .collect(Collectors.toList());
    }

    public String buildFolderPath(Folder folder) {
        if (folder == null) return "";
        StringBuilder path = new StringBuilder(folder.getName());
        Folder current = folder.getParent();
        while (current != null) {
            path.insert(0, current.getName() + "/");
            current = current.getParent();
        }
        return path.toString();
    }
}
