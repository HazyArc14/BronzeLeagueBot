package com.hazyarc14.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "season_1_archive")
public class SeasonArchive {

    @Id
    private String season;

    @Id
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "rank")
    private Double rank;

    public SeasonArchive() { }

    public SeasonArchive(String season, Long userId, String userName, Double rank) {
        this.season= season;
        this.userId = userId;
        this.userName = userName;
        this.rank = rank;
    }

    public SeasonArchive(SeasonArchive seasonArchive) {
        this.season = seasonArchive.season;
        this.userId = seasonArchive.userId;
        this.userName = seasonArchive.userName;
        this.rank = seasonArchive.rank;
    }

}