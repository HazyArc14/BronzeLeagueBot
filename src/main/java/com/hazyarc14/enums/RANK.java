package com.hazyarc14.enums;

public enum RANK {

    BRONZE("Bronze", 0.0),
    SILVER("Silver",50.0),
    GOLD("Gold",200.0),
    PLATINUM("Platinum",500.0),
    DIAMOND("Diamond",800.0),
    MASTER("Master",1200.0),
    GRANDMASTER("GrandMaster",2000.0);

    private String roleName;
    private double value;

    RANK(String roleName, double value) {
        this.roleName = roleName;
        this.value = value;
    }

    public String getRoleName() {
        return roleName;
    }

    public double getValue() {
        return value;
    }

    private static RANK[] values = values();

    public RANK previous() {
        return values[(this.ordinal() - 1  + values.length) % values.length];
    }

    public RANK next() {
        return values[(this.ordinal() + 1) % values.length];
    }

}