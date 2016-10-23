package com.gooeybar.readycheck.model;

/**
 * Created by creja_000 on 10/22/2016.
 */

public class GroupItem {

    private String groupName;
    private long numReadyMembers;
    private long numMembers;
    private State readyState;

    public GroupItem() {
        this.groupName = "";
        this.numReadyMembers = 0;
        this.numMembers = 0;
        this.readyState = State.INACTIVE;
    }

    public GroupItem(String groupName, long numReadyMembers, long numMembers, String readyState) {
        this.groupName = groupName;
        this.numReadyMembers = numReadyMembers;
        this.numMembers = numMembers;
        setReadyState(readyState);
    }

    public long getNumReadyMembers() {
        return numReadyMembers;
    }

    public void setNumReadyMembers(long numReadyMembers) {
        this.numReadyMembers = numReadyMembers;
    }

    public long getNumMembers() {
        return numMembers;
    }

    public void setNumMembers(long numMembers) {
        this.numMembers = numMembers;
    }

    public State getReadyState() {
        return readyState;
    }

    public void setReadyState(State readyState) {
        this.readyState = readyState;
    }

    public void setReadyState(String readyState) {
        if (State.READY.getStatus().equals(readyState))
            this.readyState = State.READY;
        else if (State.NOT_READY.getStatus().equals(readyState))
            this.readyState = State.NOT_READY;
        else if (State.PENDING.getStatus().equals(readyState))
            this.readyState = State.PENDING;
        else if (State.INACTIVE.getStatus().equals(readyState))
            this.readyState = State.INACTIVE;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

}
