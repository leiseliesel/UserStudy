/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.projecttango.examples.java.augmentedreality;

import com.google.atap.tangoservice.TangoPoseData;

import android.content.Context;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.EllipticalOrbitAnimation3D;
import org.rajawali3d.animation.RotateOnAxisAnimation;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.util.ObjectColorPicker;
import org.rajawali3d.util.OnObjectPickedListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.opengles.GL10;

/**
 * Renderer that implements a basic augmented reality scene using Rajawali.
 * It creates a scene with a background quad taking the whole screen, where the color camera is
 * rendered, and a sphere with the texture of the earth floating ahead of the start position of
 * the Tango device.
 */
public class AugmentedRealityRenderer extends Renderer implements OnObjectPickedListener {

    private static final String TAG = AugmentedRealityRenderer.class.getSimpleName();

    private float[] textureCoords0 = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F};
    private float[] textureCoords270 = new float[]{1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F};
    private float[] textureCoords180 = new float[]{1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F};
    private float[] textureCoords90 = new float[]{0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};

    // Rajawali texture used to render the Tango color camera.
    private ATexture mTangoCameraTexture;

    // Keeps track of whether the scene camera has been configured.
    private boolean mSceneCameraConfigured;

    private ScreenQuad mBackgroundQuad;

    private boolean firstScene = true;
    private Random rand;
    private ObjectColorPicker mPicker;

    private Object3D target;
    private ARObject arTarget;
    private int filter;
    private Material tangoCameraMaterial;
    private ReferenceObjects referenceObjects;
    private List<ARObject> unusedCombinations;
    // enthält die Distraktoren, die eine Eigenschaft mit dem Target teilen
    private List<ARObject> distractors;

    private List<Position> positions;
    private List<Position> curUsedPositions;

    public AugmentedRealityRenderer(Context context) {
        super(context);
        mPicker = new ObjectColorPicker(this);
        mPicker.setOnObjectPickedListener(this);

        // erstelle Liste von referenz Objekten. Die Objekte setzt sich aus allen Kombinationen von Farbe, Form und Filter zusammen
        // alle Objekte müssen mindestens 1* im Versuch vorgekommen sein
        referenceObjects = new ReferenceObjects();

        // erstelle Liste von bisher unbenutzten Kombinationen/Objekten.
        // werden neue Distraktoren gewürfelt, werden nur Objekte aus dieser Liste entnommen
        // um sicherzustellen, dass alle Kombinationen 1* vorkamen
        unusedCombinations = referenceObjects.getReferenceList();

        // Objekt zum erstellen randomisierter Zahlen -> wird bei würfeln des Targets, Delays, Positions und der Distraktoren verwendet
        rand = new Random();
    }

    @Override
    protected void initScene() {

        // Erstelle eine Liste von allen möglichen Positionen
        initializePositionSet();

        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.
        initializeBackgroundQuad();

        // Add a directional light in an arbitrary direction.
        addLight();

        // würfle das erste Target bevor das Spiel beginnt
        // zeige dabei eine Kurze Anleitung an
        // erstelle die Liste von Distraktoren, die jeweils eine Eigenschaft mit dem Target teilen
        // TODO show the instructions on the layout
        if(firstScene) {
            target = chooseTarget();
            distractors = getListOfDistractors();

            // TODO setup first scene with description and sample of the target
            /**
             * only the target gets displayed on the first scene*/

            target.setPosition(0, 0, -4);
            target.setDoubleSided(true);
            target.setName("Target");
            getCurrentScene().addChild(target);
            mPicker.registerObject(target);

            firstScene = false;

        } else {
            // Hier wird die Szene aufgesetzt auf welcher der Nutzer das Target zwischen den
            // Distraktoren gefunden werden soll
            // TODO setup random scene
            sceneSetup();
        }

    }

    private void initializeBackgroundQuad() {
        tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);

        if (mBackgroundQuad == null) {
            mBackgroundQuad = new ScreenQuad();
            mBackgroundQuad.getGeometry().setTextureCoords(textureCoords0);
        }
        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering
        mTangoCameraTexture =
                new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);
        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture);
            mBackgroundQuad.setMaterial(tangoCameraMaterial);
        } catch (ATexture.TextureException e) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e);
        }
        mBackgroundQuad.setName("Screen");

        getCurrentScene().addChildAt(mBackgroundQuad, 0);
        mPicker.registerObject(mBackgroundQuad);
    }

    private void addLight() {
        DirectionalLight light = new DirectionalLight(1, 0.2, -1);
        light.setColor(1, 1, 1);
        light.setPower(0.8f);
        light.setPosition(3, 2, 4);
        getCurrentScene().addLight(light);
    }

   /**
    * Initialize predefined position sets*/
    private void initializePositionSet() {
        float x = -1.2f;
        float y = -0.75f;
        float z = -3;

        positions = new ArrayList<Position>();
        Position position;

        float j;
        for(float i = y; i <= 0.75f; ) {

            for(j = x; j <= 1.2f; ) {

                DecimalFormat twoDForm = new DecimalFormat("#.##");
                j = Float.valueOf(twoDForm.format(j));

                position = new Position(j, i, z);
                positions.add(position);
                j += 0.8f;
            }
            j = x;
            i += 0.75f;
        }
    }

    private Vector3 getRandomPosition() {
        int max = positions.size() - 1;
        final int randomObj;
        Vector3 newPosition;

        randomObj = rand.nextInt(max + 1);

        newPosition = new Vector3(positions.get(randomObj).getX(),positions.get(randomObj).getY(), positions.get(randomObj).getZ());
        positions.remove(randomObj);

        return newPosition;
    }

    // Wähle ein zufälliges Target aus der referenzList
    private Object3D chooseTarget() {

        int max = referenceObjects.getReferenceList().size() - 1;
        final int randomObj = rand.nextInt(max + 1);
        arTarget = referenceObjects.getReferenceList().get(randomObj);
        Object3D newTarget = objectBuilder(arTarget);

        return newTarget;
    }

    // Erstelle ein Object3D aus ARObject
    private Object3D objectBuilder(ARObject object) {
        Object3D newObject;
        Material objectMaterial = new Material();

        if (object.getForm() == ARObject.Form.CUBE) {
            objectMaterial.setColor(object.getColor());
            newObject = new Cube(0.25f);
            newObject.setMaterial(objectMaterial);

        } else if(object.getForm() == ARObject.Form.SPHERE) {
            objectMaterial.setColor(object.getColor());
            newObject = new Sphere(0.25f, 20, 20);
            newObject.setMaterial(objectMaterial);

        } else {
            try {
                Texture t;
                if(object.getColor() == 0x000000ff) {
                    t = new Texture("icon", R.drawable.icon_blue);
                } else if (object.getColor() == 0x00ff00ff) {
                    t = new Texture("icon", R.drawable.icon_magenta);
                } else {
                    t = new Texture("icon", R.drawable.icon_cyan);
                }
                objectMaterial.addTexture(t);
            } catch (ATexture.TextureException e) {
                Log.e(TAG, "Exception generating earth texture", e);
            }
            newObject = new Plane(0.5f, 0.5f, 1, 1);
            newObject.setMaterial(objectMaterial);
            newObject.setTransparent(true);
            newObject.setColor(0x00000000);
        }

        return newObject;
    }

    /*renders the new random scene based on the given target and the used combinations
    * */
    private void sceneSetup() {
        // TODO set target

        target.setPosition(getRandomPosition());
        getCurrentScene().addChild(target);
        mPicker.registerObject(target);

        // TODO set the two distractors
        // distractor1 ist der erste Distraktor und legt den Filter fest
        // Dieser wird nur anhand der Form ausgewählt
        Object3D distractor1 = objectBuilder(getRandomDistractor(true, true));
        distractor1.setPosition(getRandomPosition());
        distractor1.setDoubleSided(true);
        distractor1.setName("Distractor");
        getCurrentScene().addChild(distractor1);
        mPicker.registerObject(distractor1);

        // setze filter
        if(filter == 0x000000) {
            tangoCameraMaterial.setColorInfluence(0);
        } else {
            tangoCameraMaterial.setColorInfluence(1);
            mBackgroundQuad.setColor(filter);
        }

        // distractor2 wird anhand des Filters von distractor1 und der Farbe ausgewählt
        Object3D distractor2 = objectBuilder(getRandomDistractor(false, true));
        distractor2.setPosition(getRandomPosition());
        distractor2.setDoubleSided(true);
        distractor2.setName("Distractor");
        getCurrentScene().addChild(distractor2);
        mPicker.registerObject(distractor2);

        // TODO get random objects
    }

    /** Returns a random distractor
     * */
    private ARObject getRandomDistractor(boolean first, boolean isDistractor) {

        ARObject distractor;

        int max;
        final int randomObj;

        // der erste distractor wird nach der gleichen form des targets ausgewählt
        // dabei wird eine neue Liste angelegt. Diese Liste beinhaltet alle noch
        // vorhandenen Distraktoren aus der distractors Liste, die die gleiche Form wie das Target haben
        if (first) {
            // Erstelle neue Liste mit distraktoren der gleichen form wie target
            List<ARObject> distractorsForm = new ArrayList<ARObject>();

            // gehe distraktoren durch und matche nach form
            for(int i = 0; i < distractors.size(); i++) {
                if(distractors.get(i).getForm() == arTarget.getForm()) {
                    distractorsForm.add(distractors.get(i));
                }
            }

            // hole objekt an zufälliger stelle in neuer Liste
            max = distractorsForm.size() - 1;
            randomObj = rand.nextInt(max + 1);

            distractor = distractorsForm.get(randomObj);

            // entferne target mit filter kombi aus unusedCombinations
            for(int i = 0; i < unusedCombinations.size(); i++) {
                if(unusedCombinations.get(i).getForm() == arTarget.getForm()
                        && unusedCombinations.get(i).getColor() == arTarget.getColor()
                        && unusedCombinations.get(i).getFilter() == filter) {
                    unusedCombinations.remove(i);
                }
            }

            filter = distractor.getFilter();

        } else if (isDistractor) {

            // Erstelle neue Liste mit distraktoren der gleichen form wie target
            List<ARObject> distractorsColor = new ArrayList<ARObject>();

            // gehe distraktoren durch und matche nach farbe des targets und filter des ersten distraktors
            for(int i = 0; i < distractors.size(); i++) {

                if(distractors.get(i).getColor() == arTarget.getColor()) {
                    distractorsColor.add(distractors.get(i));
                }

            }

            max = distractorsColor.size()-1;
            randomObj = rand.nextInt(max + 1);

            // entferne distraktor aus unusedcombinations list da kombination bereits genutzt
            distractor = distractorsColor.get(randomObj);

        } else {

            List<ARObject> matchesFilter = new ArrayList<ARObject>();

            for(int i = 0; i < unusedCombinations.size(); i++){
                if (unusedCombinations.get(i).getFilter() == filter){
                    matchesFilter.add(unusedCombinations.get(i));
                }
            }

            max = matchesFilter.size()-1;
            randomObj = rand.nextInt(max + 1);

            distractor = matchesFilter.get(randomObj);

            Log.i("Distractor", "form: " + distractor.getForm() + " color: " + distractor.getColor() + " filter: " + distractor.getFilter());

        }

        // remove distractor from unusedCombinationsList
        for(int i = 0; i < unusedCombinations.size(); i++) {
            if(unusedCombinations.get(i).getForm() == distractor.getForm()
                    && unusedCombinations.get(i).getColor() == distractor.getColor()
                    && unusedCombinations.get(i).getFilter() == distractor.getFilter()) {
                unusedCombinations.remove(i);
            }
        }

        return distractor;
    }

    /** Returns a list of ARObjects that share one property with the given target
     * */
    private List<ARObject> getListOfDistractors() {

        List<ARObject> newDistractors = new ArrayList<ARObject>();

        for(int i = 0; i < unusedCombinations.size(); i++) {

            if((unusedCombinations.get(i).getColor() == arTarget.getColor()) && (unusedCombinations.get(i).getForm() != arTarget.getForm())) {
                newDistractors.add(unusedCombinations.get(i));
            } else if((unusedCombinations.get(i).getColor() != arTarget.getColor()) && (unusedCombinations.get(i).getForm() == arTarget.getForm())) {
                newDistractors.add(unusedCombinations.get(i));
            }

        }

        for (int i = 0; i < newDistractors.size(); i++) {
            Log.i("init Distractors", "" + i + " " + newDistractors.get(i));
        }

        return newDistractors;
    }

    private void delay () {
        int maxDelay = 2000;
        final int delay = rand.nextInt(maxDelay + 1);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                // TODO render new scene (initscene)
            }};

        Handler mHandler = new Handler();
        mHandler.postDelayed(r, delay);
    }


    /**
     * Update background texture's UV coordinates when device orientation is changed. i.e change
     * between landscape and portrait mode.
     * This must be run in the OpenGL thread.
     */
    public void updateColorCameraTextureUvGlThread(int rotation) {
        if (mBackgroundQuad == null) {
            mBackgroundQuad = new ScreenQuad();
        }

        switch (rotation) {
            case Surface.ROTATION_90:
                mBackgroundQuad.getGeometry().setTextureCoords(textureCoords90);//, true);
                break;
            case Surface.ROTATION_180:
                mBackgroundQuad.getGeometry().setTextureCoords(textureCoords180);//, true);
                break;
            case Surface.ROTATION_270:
                mBackgroundQuad.getGeometry().setTextureCoords(textureCoords270);//, true);
                break;
            default:
                mBackgroundQuad.getGeometry().setTextureCoords(textureCoords0);//, true);
                break;
        }
        mBackgroundQuad.getGeometry().reload();
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The camera pose should match the pose of the camera color at the time the last rendered RGB
     * frame, which can be retrieved with this.getTimestamp();
     * <p/>
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public void updateRenderCameraPose(TangoPoseData cameraPose) {
        float[] rotation = cameraPose.getRotationAsFloats();
        float[] translation = cameraPose.getTranslationAsFloats();
        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        // Conjugating the Quaternion is need because Rajawali uses left handed convention for
        // quaternions.
        getCurrentCamera().setRotation(quaternion.conjugate());
        getCurrentCamera().setPosition(translation[0], translation[1], translation[2]);
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public int getTextureId() {
        return mTangoCameraTexture == null ? -1 : mTangoCameraTexture.getTextureId();
    }

    /**
     * We need to override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    @Override
    public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height) {
        super.onRenderSurfaceSizeChanged(gl, width, height);
        mSceneCameraConfigured = false;
    }

    public boolean isSceneCameraConfigured() {
        return mSceneCameraConfigured;
    }

    /**
     * Sets the projection matrix for the scene camera to match the parameters of the color camera,
     * provided by the {@code TangoCameraIntrinsics}.
     */
    public void setProjectionMatrix(float[] matrixFloats) {
        getCurrentCamera().setProjectionMatrix(new Matrix4(matrixFloats));
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset,
                                 float xOffsetStep, float yOffsetStep,
                                 int xPixelOffset, int yPixelOffset) {
    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    public void getObjectAt(float x, float y) {
        Log.i("getObjectAt", "x: " + x + " y: " + y);
        getCurrentScene().getNumChildren();
        mBackgroundQuad.setVisible(false);
        mPicker.getObjectAt(x, y);
    }

    @Override
    public void onObjectPicked(@NonNull Object3D object) {
        Log.i("onObjectPicked","object: " + object.getName() );
        mBackgroundQuad.setVisible(true);

        // TODO render new scene after target has been found
        if(object.getName().equals("Target")) {
            //delay();
            getCurrentScene().clearChildren();
            initScene();
        }
    }


    @Override
    public void onNoObjectPicked() {
        mBackgroundQuad.setVisible(true);
    }

}
