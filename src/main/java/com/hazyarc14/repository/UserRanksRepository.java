package com.hazyarc14.repository;

import com.hazyarc14.model.UserRank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRanksRepository extends JpaRepository<UserRank, Long> {

    List<UserRank> findAll();

}