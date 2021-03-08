package com.hazyarc14.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "season_info")
public class SeasonInfo {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "value")
    private String value;

    public SeasonInfo() { }

    public SeasonInfo(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public SeasonInfo(SeasonInfo seasonInfo) {
        this.key = seasonInfo.key;
        this.value = seasonInfo.value;
    }

}