package com.projecttango.examples.java.augmentedreality;

/**
 * Created by D066307 on 09.02.2017.
 */

public class ARObject {

    private int color;
    private int filter;
    private Form form;

    public enum Form {
        CUBE,
        SPHERE,
        ICON
    }

    public ARObject(int color, int filter, Form form) {
        this.color = color;
        this.filter = filter;
        this.form = form;
    }

    public int getColor() {
        return color;
    }

    public int getFilter() {
        return filter;
    }

    public Form getForm() {
        return form;
    }
}
