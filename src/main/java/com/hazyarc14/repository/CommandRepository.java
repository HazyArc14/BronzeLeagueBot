package com.hazyarc14.repository;

import com.hazyarc14.model.Command;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommandRepository extends JpaRepository<Command, String> {

    List<Command> findAll();

}
