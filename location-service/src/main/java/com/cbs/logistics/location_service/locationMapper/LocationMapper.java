package com.cbs.logistics.location_service.locationMapper;

import org.mapstruct.Mapper;
import com.cbs.logistics.location_service.entity.Location;
import com.cbs.logistics.location_service.dto.LocationDto;
import com.cbs.logistics.location_service.dto.CreateLocationRequest;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    LocationDto toDto(Location entity);

    Location toEntity(CreateLocationRequest request);
}
