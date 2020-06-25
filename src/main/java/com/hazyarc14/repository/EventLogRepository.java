package com.hazyarc14.repository;

import com.hazyarc14.model.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    List<EventLog> findAll();

}