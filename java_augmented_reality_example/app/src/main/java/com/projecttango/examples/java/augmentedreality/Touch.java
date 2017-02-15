package com.projecttango.examples.java.augmentedreality;

/**
 * Created by D066307 on 15.02.2017.
 */

public class Touch {
    private Position position;
    private long time;
    private String object;

    public Touch(Position position, long time, String object) {
        this.position = position;
        this.time = time;
        this.object = object;
    }

    public String getTouch() {
        return "object: " + object + ", time: " + time + ", position: " + position;
    }

}
