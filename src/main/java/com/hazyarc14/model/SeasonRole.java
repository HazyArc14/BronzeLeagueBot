package com.hazyarc14.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@IdClass(SeasonRoleId.class)
@Table(name = "season_roles")
public class SeasonRole {

    @Id
    @Column(name = "season")
    private String season;

    @Id
    @Column(name = "role_name")
    private String roleName;

    @Column(name = "role_value")
    private double roleValue;

    @Column(name = "role_order")
    private Integer roleOrder;

    public SeasonRole() { }

    public SeasonRole(String season, String roleName, double roleValue, Integer roleOrder) {
        this.season = season;
        this.roleName = roleName;
        this.roleValue = roleValue;
        this.roleOrder = roleOrder;
    }

    public SeasonRole(SeasonRole seasonInfo) {
        this.season = seasonInfo.season;
        this.roleName = seasonInfo.roleName;
        this.roleValue = seasonInfo.roleValue;
        this.roleOrder = seasonInfo.roleOrder;
    }

}