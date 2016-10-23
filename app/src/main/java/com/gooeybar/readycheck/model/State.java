package com.gooeybar.readycheck.model;

/**
 * Created by creja_000 on 10/22/2016.
 */

public enum State {
    READY("ready"),
    NOT_READY("not_ready"),
    PENDING("pending"),
    INACTIVE("inactive");

    private String statusValue;

    State(String statusValue) {
        this.statusValue = statusValue;
    }

    public String getStatus() {
        return statusValue;
    }
}
