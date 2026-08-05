package com.cbs.logistics.package_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Data

public class Package {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private Long packageId;
    @Column
    private String description;
    @Column
    private String packageName;
    @Column
    private String packageType;
    @Column
    private Double weight;
    @Column
    private boolean fragile;
    @Column
    private PackageStatus packageStatus;
    @Column
    private String locationId;

}
