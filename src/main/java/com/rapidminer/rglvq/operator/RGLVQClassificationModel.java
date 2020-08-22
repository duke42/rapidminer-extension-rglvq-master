/**
 * Duc Anh Nguyen
 *
 * Copyright (C) 2020-2020 by Duc Anh Nguyen and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      www.rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.rglvq.operator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;


public class RGLVQClassificationModel<T extends Serializable> extends PredictionModel {


    private boolean debugMode = false;
    private static final long serialVersionUID = 1L;
    private ExampleSet prototypes;
    private double[][] squaredDissMatrix;
    DissimilarityMeasure measure;


    //private double sigma;

    protected RGLVQClassificationModel(ExampleSet prototypes, ExampleSet trainingSet, double[][] dissimilarityMatrix ) {//
        super(trainingSet, ExampleSetUtilities.SetsCompareOption.EQUAL, ExampleSetUtilities.TypesCompareOption.EQUAL);
        this.prototypes = prototypes;
        this.squaredDissMatrix = dissimilarityMatrix;
        this.measure = new DissimilarityMeasure(this.squaredDissMatrix);
    }




    @Override
    public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {

        //DissimilarityMeasure measure = new DissimilarityMeasure(dissimilarityMatrix);
        double distance = 0;
        for (Example example : exampleSet) {
            double[][] labelDistances = new double[prototypes.size()][2];
            double[] exampleValues = new double[example.getAttributes().size()];

            //cache predictionSet values into array
            int i = 0;
            for (Attribute a : example.getAttributes()) {
                exampleValues[i] = example.getValue(a);
                i++;
            }
            //cache optimized prototype values into array
            i = 0;
            //DissimilarityMeasure dissimilarity = new DissimilarityMeasure(dissimilarityMatrix);
            for (Example p : prototypes) {
                double[] optPrototypes = new double[p.getAttributes().size()];
                int j = 0;
                for (Attribute a : p.getAttributes()) {
                    optPrototypes[j] = p.getValue(a);
                    j++;
                }
                labelDistances[i][0] = measure.calculateDistance(optPrototypes, exampleValues);//dissimilarityMatrix
                labelDistances[i][1] = p.getLabel();
                i++;
            }
            int minIndex = 0;
            double minDistance = Double.MAX_VALUE;
            i = 0;
            for (double[] dist : labelDistances) {
                if (dist[0] < minDistance) {
                    minIndex = i;
                    minDistance = dist[0];
                }
                i++;
            }
            HashMap<Double, Double> maxDistancePerClass = new HashMap<Double, Double>();
            for (double[] dis : labelDistances) {
                maxDistancePerClass.put(dis[1], dis[0]);
            }
            for (double[] dis : labelDistances) {
                if (maxDistancePerClass.get(dis[1]) > dis[0]) {
                    maxDistancePerClass.put(dis[1], dis[0]);
                }
            }

            example.setValue(predictedLabel, labelDistances[minIndex][1]);
        }
        return exampleSet;
    }


    // Erzeugt den Text, der f�r das Model in der Result Sicht angezeigt wird
    @Override
    public String toString() {
        StringBuilder description = new StringBuilder();
        description.append(super.toString());
        description.append("\n");
        description.append("\n" + "Classification is based on Nearest Prototype Classification. Prototypes are created using RSLVQ Algorithm.\n \n");
        int i = 1;
        for (Example cb : prototypes){
            description.append("Prototype " + i + ":      ");
            description.append("Label = " + cb.getAttributes().getLabel().getName() + ": " +cb.getLabel() + "; ");
            for (Attribute att : prototypes.getAttributes()) {
                description.append(att.getName() + ": " + cb.getValue(att) + "; ");
            }
            description.append("\n");
            i++;
        }
        return description.toString();
    }







}
















