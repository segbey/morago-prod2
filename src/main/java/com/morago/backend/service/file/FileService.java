package com.morago.backend.service.file;

import com.morago.backend.dto.FileResponse;
import org.springframework.web.multipart.MultipartFile;
import com.morago.backend.entity.File;
import com.morago.backend.entity.enumFiles.FileCategory;

import java.util.Optional;


public interface FileService {
    FileResponse uploadAvatar(Long userId, MultipartFile file);
    FileResponse uploadMyAvatar(MultipartFile file);

    void deleteMyAvatar();
    void deleteAvatar(Long userId);

    FileResponse uploadThemeIcon(Long themeId, MultipartFile file);

    FileResponse uploadTranslatorDoc(Long translatorId, MultipartFile file);
    FileResponse uploadMyTranslatorDoc(MultipartFile file);
    Optional<File> findByUserIdAndCategory(Long userId, FileCategory category);
}
