package com.lms.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Table {@code Role} — entity ROLE. */
@Entity
@Table(name = "Role")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleID")
    @Setter(AccessLevel.NONE)
    private Integer roleId;

    @Column(name = "RoleName", nullable = false, length = 50)
    private String roleName;

    @Column(name = "RoleDescription", length = 255)
    private String roleDescription;

    @Column(name = "IsActive", nullable = false)
    private boolean active = true;
}
