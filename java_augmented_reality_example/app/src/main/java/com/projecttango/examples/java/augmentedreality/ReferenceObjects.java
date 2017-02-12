package com.projecttango.examples.java.augmentedreality;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by D066307 on 09.02.2017.
 */

public class ReferenceObjects {

    private List<ARObject> referenceList;
    private List<Integer> colors;
    private List<Integer> filters;
    private List<ARObject.Form> forms;

    public ReferenceObjects() {
        colors = initListOfColors();
        filters = initListOfFilters();
        forms = getListOfForms();
        referenceList = initObjects();
    }


    /*
    * Initialization methods of variables
    * */
    private List<ARObject> initObjects() {

        List<ARObject> objects = new ArrayList<ARObject>();
        ARObject object;

        // add color
        for (int i = 0; i < colors.size(); i++) {

            // add form
            for(int j = 0; j < forms.size(); j++ ) {

                // add filter
                for(int k = 0; k < filters.size(); k++) {
                    object = new ARObject(colors.get(i), filters.get(j), forms.get(k));
                    Log.i("Reference objects", "added " + object.getColor() + " " + object.getFilter() + " " + object.getForm());

                    objects.add(object);
                }
            }
        }
        Log.i("Num of objects", "num:" + objects.size());

        return objects;
    }

    public List<ARObject.Form> getListOfForms() {
        List<ARObject.Form> formList = new ArrayList<ARObject.Form>();

        formList.add(ARObject.Form.CUBE);
        formList.add(ARObject.Form.SPHERE);
        formList.add(ARObject.Form.ICON);

        return formList;
    }


    private List<Integer> initListOfColors() {
        List<Integer> colorList = new ArrayList<Integer>();

        colorList.add(0x00ff00ff);// Magenta
        colorList.add(0x000000ff);// Blue
        colorList.add(0x0000ffff);// Cyan

        return colorList;
    }

    private List<Integer> initListOfFilters() {
        List<Integer> filterList = new ArrayList<Integer>();

        filterList.add(0x007b4208);// Sepia saturized
        filterList.add(0x0064411f);// Sepia unsaturized
        filterList.add(0x00000000);// No filter

        return filterList;
    }
     public List<ARObject> getReferenceList() {
         return referenceList;
     }
}
