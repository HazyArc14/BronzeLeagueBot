package com.hazyarc14.model;

import lombok.Data;

import javax.persistence.Entity;
import java.io.Serializable;

@Entity
@Data
public class SeasonRoleId implements Serializable {

    private String season;
    private String roleName;

    public SeasonRoleId() { }

    public SeasonRoleId(String season, String roleName) {
        this.season = season;
        this.roleName = roleName;
    }

}