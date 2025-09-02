package com.morago.backend.mapper;

import com.morago.backend.config.GlobalMappingConfig;
import com.morago.backend.dto.DepositDto;
import com.morago.backend.entity.Deposit;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "spring",
        config = GlobalMappingConfig.class,
        uses   = { MoneyMapper.class, RefMappers.class })
public interface DepositMapper {

    @Mapping(source = "user.id", target = "userId")
    DepositDto toDto(Deposit deposit);

    /* ---------- DTO -> Entity (create/replace) ---------- */
    @Mapping(source = "userId", target = "user")
    // @Mapping(target = "status", ignore = true)
    @Mapping(target = "coinDecimal", qualifiedByName = "scale2")
    @Mapping(target = "wonDecimal",  qualifiedByName = "scale2")
    Deposit toEntity(DepositDto dto);

    @AfterMapping
    default void normalizeMoney(@MappingTarget Deposit e, @Context MoneyMapper money) {
        e.setCoinDecimal(money.scale2(e.getCoinDecimal()));
        e.setWonDecimal(money.scale2(e.getWonDecimal()));
    }
}

