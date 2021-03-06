package com.rear_admirals.york_pirates;

import java.io.Serializable;
import java.util.ArrayList;

public class College implements Serializable {

	private final String name;
	private ArrayList<College> ally;
    private boolean bossDead;

    private College(String name) {
        this.name = name;
        this.ally = new ArrayList<College>();
        this.ally.add(this);
        this.bossDead = false;
    }

    public String getName() { return name; }

    public ArrayList<College> getAlly() { return ally; }
    public void addAlly(College newAlly){
        ally.add(newAlly);
    }

    public boolean isBossDead() {
        return bossDead;
    }
    public void setBossDead(boolean bossDead) {
        this.bossDead = bossDead;
    }

	public static College Derwent = new College("Derwent");
    public static College Vanbrugh = new College("Vanbrugh");
    public static College James = new College("James");
    public static College Alcuin = new College("Alcuin");
    public static College Wentworth = new College("Wentworth");
}
