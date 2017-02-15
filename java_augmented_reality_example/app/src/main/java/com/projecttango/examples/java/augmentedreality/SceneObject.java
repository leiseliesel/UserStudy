package com.projecttango.examples.java.augmentedreality;

import org.rajawali3d.math.vector.Vector3;

/**
 * Created by D066307 on 14.02.2017.
 */

public class SceneObject {

    private ARObject object;
    private Vector3 position;

    public SceneObject(ARObject object, Vector3 position) {
        this.object = object;
        this.position = position;
    }

    public ARObject getObject() {
        return object;
    }

    public Vector3 getPosition() {
        return position;
    }

}
