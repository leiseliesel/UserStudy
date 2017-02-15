package com.projecttango.examples.java.augmentedreality;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by D066307 on 13.02.2017.
 */

public class Scene {
    private int sceneNr;
    private SceneObject target;
    private List<SceneObject> distractors;
    private long reactionTime;
    private int filter;
    private List<Touch> missedTips;

    public Scene(int sceneNr, SceneObject target, int filter) {
        this.sceneNr = sceneNr;
        this.target = target;
        this.filter = filter;
        distractors = new ArrayList<SceneObject>();
        missedTips = new ArrayList<Touch>();
    }

    public void addDistractor(SceneObject distractor) {
        distractors.add(distractor);
    }

    public void setReactionTime(long reactionTime) {
        this.reactionTime = reactionTime;
    }

    public void addTouch(Touch touch) {
        missedTips.add(touch);
    }

    public long getReactionTime() {
        return reactionTime;
    }

    public int getFilter() {
        return filter;
    }

    public SceneObject getTarget() {
        return target;
    }

    public List<SceneObject> getDistractors() {
        return distractors;
    }

    public List<Touch> getMissedTips() {
        return missedTips;
    }
}


