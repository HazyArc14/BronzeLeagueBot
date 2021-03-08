package com.hazyarc14.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class SeasonArchiveId implements Serializable {

    private String season;
    private Long userId;

    public SeasonArchiveId() { }

    public SeasonArchiveId(String season, Long userId) {
        this.season= season;
        this.userId = userId;
    }

}