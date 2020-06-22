package com.hazyarc14.repository;

import com.hazyarc14.model.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    List<UserLog> findAll();

}