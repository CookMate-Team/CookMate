package com.cookmate.main.mapper;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.model.Step;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StepMapper {

    /**
     * Mapuje Encję na DTO (dla frontendu).
     * MapStruct automatycznie sprawdza, czy obiekt 'step' nie jest nullem.
     */
    StepDTO toDTO(Step step);

    /**
     * Mapuje DTO na Encję (do zapisu w bazie).
     * Ignorujemy createdAt, bo to pole powinno być sterowane przez mechanizmy bazy/JPA.
     */
    @Mapping(target = "createdAt", ignore = true)
    Step toEntity(StepDTO stepDTO);
}