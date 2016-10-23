package com.gooeybar.readycheck.model;

import android.graphics.drawable.Drawable;

/**
 * Created by creja_000 on 10/22/2016.
 */

public class MemberItem {
    private String memberName;
    private State readyState;

    public MemberItem() {
        this.memberName = "";
        this.readyState = State.INACTIVE;
    }

    public MemberItem(String memberName, String readyState) {
        this.memberName = memberName;
        setReadyState(readyState);
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

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
}
