package com.hazyarc14.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "user_info")
public class UserInfo {

    @Id
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "rank")
    private Double rank;

    @Column(name = "joined_channel_tm")
    private Timestamp joinedChannelTm;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "steam_id")
    private Long steamId;

    public UserInfo() { }

    public UserInfo(Long userId, String userName, Double rank, Timestamp joinedChannelTm, Boolean active, Long steamId) {
        this.userId = userId;
        this.userName = userName;
        this.rank = rank;
        this.joinedChannelTm = joinedChannelTm;
        this.active = active;
        this.steamId = steamId;
    }

    public UserInfo(UserInfo user) {
        this.userId = user.userId;
        this.userName = user.userName;
        this.rank = user.rank;
        this.joinedChannelTm = user.joinedChannelTm;
        this.active = user.active;
        this.steamId = user.steamId;
    }

}