package com.cbs.logistics.security_checkpoint_service.mapper;

import com.cbs.logistics.security_checkpoint_service.dto.CheckpointLogDto;
import com.cbs.logistics.security_checkpoint_service.dto.CreateCheckpointRequest;
import com.cbs.logistics.security_checkpoint_service.entity.CheckpointLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CheckpointLogMapper {

    CheckpointLogDto toDto(CheckpointLog entity);

    @Mapping(target = "id", ignore = true)
    CheckpointLog toEntity(CreateCheckpointRequest request);
}