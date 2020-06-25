package com.hazyarc14.repository;

import com.hazyarc14.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {

    List<UserInfo> findAll();

}