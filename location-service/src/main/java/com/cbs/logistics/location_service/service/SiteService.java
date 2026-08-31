package com.cbs.logistics.location_service.service;

import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.location_service.dto.CreateSiteRequest;
import com.cbs.logistics.location_service.dto.SiteDto;
import com.cbs.logistics.location_service.entity.Site;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;

    public SiteDto create(CreateSiteRequest request) {
        Site site = Site.builder()
                .tenantId(TenantContext.getCurrent())
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .active(true)
                .build();
        Site saved = siteRepository.save(site);
        return toDto(saved);
    }

    public SiteDto getById(Long id) {
        String tenantId = TenantContext.getCurrent();
        Site site = siteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocationNotFoundException("Site not found with id: " + id));
        return toDto(site);
    }

    public Page<SiteDto> getAll(Pageable pageable) {
        String tenantId = TenantContext.getCurrent();
        return siteRepository.findByTenantId(tenantId, pageable).map(this::toDto);
    }

    public SiteDto update(Long id, CreateSiteRequest request) {
        String tenantId = TenantContext.getCurrent();
        Site site = siteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocationNotFoundException("Site not found with id: " + id));
        site.setName(request.name());
        site.setAddress(request.address());
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        Site saved = siteRepository.save(site);
        return toDto(saved);
    }

    public void delete(Long id) {
        String tenantId = TenantContext.getCurrent();
        Site site = siteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocationNotFoundException("Site not found with id: " + id));
        site.setActive(false);
        siteRepository.save(site);
    }

    private SiteDto toDto(Site site) {
        return new SiteDto(site.getId(), site.getName(), site.getAddress(),
                site.getLatitude(), site.getLongitude(), site.getActive());
    }
}
