package com.hazyarc14.model;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Data
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

}