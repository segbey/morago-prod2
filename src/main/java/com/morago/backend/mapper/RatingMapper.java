package com.morago.backend.mapper;

import com.morago.backend.dto.RatingUpsertRequest;
import com.morago.backend.entity.Rating;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RatingMapper {
    void updateEntityFromDto(RatingUpsertRequest dto, @MappingTarget Rating entity);
}
