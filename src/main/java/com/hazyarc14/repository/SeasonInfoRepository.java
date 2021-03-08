package com.hazyarc14.repository;

import com.hazyarc14.model.SeasonInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonInfoRepository extends JpaRepository<SeasonInfo, String> {

    List<SeasonInfo> findAll();

}