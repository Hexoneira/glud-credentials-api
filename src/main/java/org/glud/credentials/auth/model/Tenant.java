package org.glud.credentials.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tenantId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String tenantCode;
    @Column(nullable = false)
    private String director;
    @Column(nullable = false)
    private Integer memberLimit;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;
    @Column(nullable = false, length = 20)
    private String primaryColor = "#22fefb";
    private String logoUrl;
}
