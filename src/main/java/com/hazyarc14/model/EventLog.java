package com.hazyarc14.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "event_log")
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "type")
    private String type;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "tm")
    private Timestamp tm;

    @Column(name = "message")
    private String message;

    @Column(name = "rank")
    private Double rank;

    public EventLog() { }

    public EventLog(EventLog event) {
        this.oid = event.oid;
        this.type = event.type;
        this.userName = event.userName;
        this.userId = event.userId;
        this.tm = event.tm;
        this.message = event.message;
        this.rank = event.rank;
    }

    public Long getOid() {
        return oid;
    }
    public void setOid(Long oid) {
        this.oid = oid;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Timestamp getTm() {
        return tm;
    }
    public void setTm(Timestamp tm) {
        this.tm = tm;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public Double getRank() {
        return this.rank;
    }
    public void setRank(Double rank) {
        this.rank = rank;
    }

}