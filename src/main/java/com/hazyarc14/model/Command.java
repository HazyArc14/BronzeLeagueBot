package com.hazyarc14.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "commands")
public class Command {

    @Id
    private String commandName;
    private byte[] commandFile;
    private String commandFileExtension;
    private Boolean active;

    public Command() { }

    public Command(String name, byte[] commandUrl, String commandFileExtension, Boolean active) {
        this.commandName = name;
        this.commandFile = commandUrl;
        this.commandFileExtension = commandFileExtension;
        this.active = active;
    }

}
