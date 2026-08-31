package com.cbs.logistics.location_service.service;

import com.cbs.logistics.common.security.context.TenantContext;
import com.cbs.logistics.location_service.dto.CreateSiteRequest;
import com.cbs.logistics.location_service.dto.SiteDto;
import com.cbs.logistics.location_service.entity.Site;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.repository.SiteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    private static final String TENANT_ID = "test-tenant";

    @Mock private SiteRepository siteRepository;
    @InjectMocks private SiteService siteService;

    @BeforeEach
    void setUp() { TenantContext.setCurrent(TENANT_ID); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void create_shouldSetTenantId() {
        CreateSiteRequest request = new CreateSiteRequest("Site A", "123 rue", 48.8, 2.3);
        Site site = Site.builder().id(1L).tenantId(TENANT_ID).name("Site A").active(true).build();
        when(siteRepository.save(any())).thenReturn(site);

        SiteDto result = siteService.create(request);

        assertThat(result.name()).isEqualTo("Site A");
        verify(siteRepository).save(any());
    }

    @Test
    void getById_shouldReturn404_whenNotFound() {
        when(siteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> siteService.getById(1L)).isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    void getAll_shouldReturnOwnTenantData() {
        Site site = Site.builder().id(1L).tenantId(TENANT_ID).name("Site A").active(true).build();
        Page<Site> page = new PageImpl<>(List.of(site), PageRequest.of(0, 10), 1);
        when(siteRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10))).thenReturn(page);

        var result = siteService.getAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void delete_shouldSoftDelete() {
        Site site = Site.builder().id(1L).tenantId(TENANT_ID).name("Site A").active(true).build();
        when(siteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(site));
        when(siteRepository.save(any())).thenReturn(site);

        siteService.delete(1L);

        assertThat(site.getActive()).isFalse();
    }
}
