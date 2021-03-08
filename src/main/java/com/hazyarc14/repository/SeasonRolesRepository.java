package com.hazyarc14.repository;

import com.hazyarc14.model.SeasonRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonRolesRepository extends JpaRepository<SeasonRole, String> {

    List<SeasonRole> findAll();

    List<SeasonRole> findAllBySeason(String season);

    List<SeasonRole> findAllBySeasonAndRoleOrder(String season, Integer roleOrder);

}