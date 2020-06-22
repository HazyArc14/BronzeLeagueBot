package com.hazyarc14.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "user_log")
public class UserLog {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "old_rank")
    private Double oldRank;

    @Column(name = "new_rank")
    private Double newRank;

    @Column(name = "method_call")
    private String methodCall;

    @Column(name = "update_tm")
    private Timestamp updateTm;

    @Column(name = "active")
    private Boolean active;

    public UserLog() { }

    public UserLog(Long userId, String userName, Double oldRank, Double newRank, String methodCall, Timestamp updateTm, Boolean active) {
        this.userId = userId;
        this.userName = userName;
        this.oldRank = oldRank;
        this.newRank = newRank;
        this.methodCall = methodCall;
        this.updateTm = updateTm;
        this.active = active;
    }

    public UserLog(UserLog user) {
        this.userId = user.userId;
        this.userName = user.userName;
        this.oldRank = user.oldRank;
        this.newRank = user.newRank;
        this.methodCall = user.methodCall;
        this.updateTm = user.updateTm;
        this.active = user.active;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() { return this.userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Double getOldRank() {
        return this.oldRank;
    }
    public void setOldRank(Double oldRank) {
        this.oldRank = oldRank;
    }

    public Double getNewRank() {
        return this.newRank;
    }
    public void setNewRank(Double newRank) {
        this.newRank = newRank;
    }

    public String getMethodCall() { return this.getMethodCall(); }
    public void setMethodCall(String methodCall) { this.methodCall = methodCall; }

    public Timestamp getUpdateTm() {
        return this.updateTm;
    }
    public void setUpdateTm(Timestamp updateTm) {
        this.updateTm = updateTm;
    }

    public Boolean getActive() {
        return this.active;
    }
    public void setActive(Boolean active) {
        this.active = active;
    }

}
