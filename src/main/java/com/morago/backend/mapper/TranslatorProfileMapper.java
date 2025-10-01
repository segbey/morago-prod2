package com.morago.backend.mapper;

import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.entity.Language;
import com.morago.backend.entity.Theme;
import com.morago.backend.entity.TranslatorProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TranslatorProfileMapper {

    @Mapping(source = "user.id",   target = "userId")
    @Mapping(source = "languages", target = "languageIds", qualifiedByName = "toLanguageIdSet")
    @Mapping(source = "themes",    target = "themeIds",    qualifiedByName = "toThemeIdSet")
    TranslatorProfileDto toDto(TranslatorProfile profile);

    @Named("toLanguageIdSet")
    static Set<Long> toLanguageIdSet(Set<Language> langs) {
        if (langs == null || langs.isEmpty()) return Set.of();
        return langs.stream().map(Language::getId).collect(Collectors.toSet());
    }

    @Named("toThemeIdSet")
    static Set<Long> toThemeIdSet(Set<Theme> themes) {
        if (themes == null || themes.isEmpty()) return Set.of();
        return themes.stream().map(Theme::getId).collect(Collectors.toSet());
    }
}
