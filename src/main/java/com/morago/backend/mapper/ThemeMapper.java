package com.morago.backend.mapper;

import com.morago.backend.dto.ThemeDto;
import com.morago.backend.entity.Theme;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = {TranslatorProfileMapper.class})
public interface ThemeMapper {

    ThemeMapper INSTANCE = Mappers.getMapper(ThemeMapper.class);

    @Mapping(target = "popular",     expression = "java(theme.isPopular())")
    @Mapping(target = "active",      expression = "java(theme.isActive())")
    @Mapping(target = "categoryId",
            expression = "java(theme.getCategory() != null ? theme.getCategory().getId() : null)")
    @Mapping(target = "iconUrl", expression = "java(theme.getIcon() != null ? theme.getIcon().getPath() : null)")
    ThemeDto toDto(Theme theme);

    Theme toEntity(ThemeDto dto);
}