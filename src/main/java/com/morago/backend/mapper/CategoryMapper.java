package com.morago.backend.mapper;

import com.morago.backend.dto.CategoryDto;
import com.morago.backend.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    @Mapping(target = "active",    expression = "java(category.isActive())")
    CategoryDto toDto(Category category);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", expression = "java(Boolean.TRUE.equals(dto.getActive()))")
    Category toEntity(CategoryDto dto);

    List<CategoryDto> toDtoList(List<Category> categories);
}
