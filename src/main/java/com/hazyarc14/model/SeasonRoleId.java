package com.hazyarc14.model;

import lombok.Data;

import java.io.Serializable;

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