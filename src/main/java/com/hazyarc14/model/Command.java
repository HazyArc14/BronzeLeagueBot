package com.hazyarc14.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
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

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }
    public String getCommandName() {
        return this.commandName;
    }

    public void setCommandFile(byte[] commandFile) {
        this.commandFile = commandFile;
    }
    public byte[] getCommandFile() {
        return this.commandFile;
    }

    public void setCommandFileExtension(String commandFileExtension) {
        this.commandFileExtension = commandFileExtension;
    }
    public String getCommandFileExtension() {
        return this.commandFileExtension;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
    public Boolean getActive() {
        return this.active;
    }

}
