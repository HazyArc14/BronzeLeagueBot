package com.hazyarc14.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "user_ranks")
public class UserRank {

    @Id
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "rank")
    private Double rank;

    @Column(name = "joined_channel_tm")
    private Timestamp joinedChannelTm;

    @Column(name = "left_channel_tm")
    private Timestamp leftChannelTm;

    public UserRank() { }

    public UserRank(Long userId, String userName, Double rank, Timestamp joinedChannelTm, Timestamp leftChannelTm) {
        this.userId = userId;
        this.userName = userName;
        this.rank = rank;
        this.joinedChannelTm = joinedChannelTm;
        this.leftChannelTm = leftChannelTm;
    }

    public UserRank(UserRank user) {
        this.userId = user.userId;
        this.userName = user.userName;
        this.rank = user.rank;
        this.joinedChannelTm = user.joinedChannelTm;
        this.leftChannelTm = user.leftChannelTm;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return this.userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Double getRank() {
        return this.rank;
    }
    public void setRank(Double rank) {
        this.rank = rank;
    }

    public Timestamp getJoinedChannelTm() {
        return this.joinedChannelTm;
    }
    public void setJoinedChannelTm(Timestamp joinedChannelTm) {
        this.joinedChannelTm = joinedChannelTm;
    }

    public Timestamp getLeftChannelTm() {
        return this.leftChannelTm;
    }
    public void setLeftChannelTm(Timestamp leftChannelTm) {
        this.leftChannelTm = leftChannelTm;
    }

}
