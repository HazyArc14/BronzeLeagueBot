package com.hazyarc14.repository;

import com.hazyarc14.model.SeasonArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonArchiveRepository extends JpaRepository<SeasonArchive, Long> {

    List<SeasonArchive> findAll();

}