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
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import edu.uci.ics.jung.algorithms.shortestpath.Distance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class RGLVQModel extends AbstractModel{


    private final int iterations; //Total number of iterations
    private int currentIteration; //Iteration id
    private double alpha; //Learning rate
    private double time; //Time used in sigmoidal cost function evaluation
    private final double initialAlpha; // Initial value of alpha, here alpha is not changing
    private final boolean debug; //In debug mode the true cost function is calculated
    private double costValue; //The value of the cost function
    private final List<Double> costValues; //List of cost Function values
    private final List<Double> learningRateValues; //List of learning rate values
    private final List<Double> timeRateValues; //List of time (lambda) rate values
    private final double timeMax = 10;
    private final double timeMin = 1;
    //private final LearningRateUpdateRule learningRateUpdateRule; //The update rule of the learning rate
    private int numberOfUpdates; //The number of times the update function was executed before nextIteration was executed
    private double tempFactor = 0;
    DissimilarityMeasure measure;


    /**
     * Constructor
     *
     * @param prototypes
     * @param iterations
     * @param alpha
     * @param
     * @throws OperatorException
     */
    public RGLVQModel(ExampleSet prototypes, int iterations, double alpha, double[][] dissimilarityMatrix) throws OperatorException {
        super(prototypes);
        this.iterations = iterations;
        this.currentIteration = 0;
        this.alpha = alpha;
        this.time = 1;
        this.initialAlpha = alpha;
        this.costValues = new ArrayList<>(iterations);
        this.debug = false;
        this.squaredDissMatrix = dissimilarityMatrix;
        this.measure = new DissimilarityMeasure(dissimilarityMatrix);
        learningRateValues = new ArrayList<>(iterations);
        timeRateValues = new ArrayList<>(iterations);
    }



    /**
     *
     */
    public void update() {

        double distance;
        double closestCorrect = Double.MAX_VALUE;
        double closestIncorrect = Double.MAX_VALUE;
        int rowCorrectPrototypes = 0;
        int rowIncorrectPrototypes = 0;
        int i = 0;

        for (double[] prototype : prototypeValues) {
            distance = measure.calculateDistance(prototype, exampleValues);
            //LogService.getRoot().log(Level.INFO, "Ducanh:distance: " + distance);
            double protoLabel = prototypeLabels[i];

            if (distance < closestCorrect && exampleLabel == protoLabel) {
                closestCorrect = distance;
                rowCorrectPrototypes = i;
            }
            if (distance < closestIncorrect && exampleLabel != protoLabel) {
                closestIncorrect = distance;
                rowIncorrectPrototypes = i;
            }
            i++;
        }

        double denominator = closestCorrect + closestIncorrect + 1E-5;
        //LogService.getRoot().log(Level.INFO, "Ducanh:denominator: " + denominator);
        denominator = denominator == 0 ? 1e-10 : denominator;
        double mu = (closestCorrect - closestIncorrect) / denominator;
        //LogService.getRoot().log(Level.INFO, "Ducanh:closestCorrect " + closestCorrect);
        //LogService.getRoot().log(Level.INFO, "Ducanh:closestInCorrect " + closestIncorrect);
        //LogService.getRoot().log(Level.INFO, "Ducanh:mu " + mu);
        double currentCostValue = mu;
        costValue += currentCostValue;
        double muSigmoid = currentCostValue * (1 - currentCostValue);
        //LogService.getRoot().log(Level.INFO, "Ducanh:muSigmoud " + muSigmoid);
        double muCorrect =   closestIncorrect / (denominator * denominator);
        double muIncorrect = closestCorrect / (denominator * denominator);
        //tempFactor += 2 * alpha * muSigmoid * muCorrect;
        //LogService.getRoot().log(Level.INFO, "Ducanh:muCorrect " + muCorrect);
        //LogService.getRoot().log(Level.INFO, "Ducanh:muIncorrect " + muIncorrect);
        double[] matrixVectorCorrect = new double[squaredDissMatrix[0].length];
        double[] matrixVectorIncorrect = new double[squaredDissMatrix[0].length];
        int l = 0;

        for (int j = 0; j < squaredDissMatrix.length; j++) {
            for (int k = 0; k < squaredDissMatrix[0].length; k++) {
                matrixVectorCorrect[l] += squaredDissMatrix[j][k] * prototypeValues[rowCorrectPrototypes][k];
                matrixVectorIncorrect[l] += squaredDissMatrix[j][k] * prototypeValues[rowIncorrectPrototypes][k];
                //LogService.getRoot().log(Level.INFO, "Ducanh:matrixVectorCorrect " + matrixVectorCorrect[l]);
                //LogService.getRoot().log(Level.INFO, "Ducanh:matrixVectorIncorrect " + matrixVectorIncorrect[l]);
            }
            l++;
        }

        for (i = 0; i < getAttributesSize(); i++) {
            double trainValue = exampleValues[i];
            double valueCorrect = prototypeValues[rowCorrectPrototypes][i];
            double valueIncorrect = prototypeValues[rowIncorrectPrototypes][i];
            valueCorrect += 2 * alpha * muSigmoid  * muCorrect * ( (trainValue - matrixVectorCorrect[i]) - valueCorrect );
            valueIncorrect -= 2 * alpha * muSigmoid * muIncorrect * ( (trainValue - matrixVectorIncorrect[i]) - valueIncorrect);
            //LogService.getRoot().log(Level.INFO, "Ducanh:matrixVectorCorrect2 " + matrixVectorCorrect[i]);
            //LogService.getRoot().log(Level.INFO, "Ducanh:matrixVectorIncorrect2 " + matrixVectorIncorrect[i]);
            prototypeValues[rowCorrectPrototypes][i] = valueCorrect;
            prototypeValues[rowIncorrectPrototypes][i] = valueIncorrect;
            //LogService.getRoot().log(Level.INFO, "Ducanh:valueCorrect: " + valueCorrect);
            //LogService.getRoot().log(Level.INFO, "Ducanh:valueInCorrect: " + valueIncorrect);

        }
        numberOfUpdates++;
        LogService.getRoot().log(Level.INFO, "Ducanh:numberOfUpdates " + numberOfUpdates);
    }

    /**
     * Returns true if the algorithm should perform the next iteration step
     * @param trainingSet - training ExampleSet
     * @return - wether the next Iteration should be processed or not
     */
    public boolean nextIteration(ExampleSet trainingSet) {
        currentIteration++;
        //LogService.getRoot().log(Level.INFO, "Ducanh: CurrentIteration " + currentIteration);
        //if (debug) {
            //costValues.add(calcCostFunction(trainingSet));
        //} else {
        //LogService.getRoot().log(Level.INFO, "Ducanh:alpha " + alpha);
        numberOfUpdates = 0;
        alpha = alpha / (1 + alpha);

        return currentIteration < iterations;

    }

    /**
     *
     * @param trainingSet - training ExampleSet
     * @return - costFunction
     */
    /*public double calcCostFunction(ExampleSet trainingSet) {
        double[] tempExampleValues = new double[getAttributesSize()];
        double tempExampleLabel;
        double costValue = 0;
        for (Example e : trainingSet) {
            int j = 0;
            for (Attribute attribute : trainingAttributes) {
                tempExampleValues[j] = e.getValue(attribute);
                j++;
            }
            tempExampleLabel = e.getLabel();
            double dist;
            double minDistCorrect = Double.MAX_VALUE;
            double minDistIncorrect = Double.MAX_VALUE;
            int i = 0;
            DissimilarityMeasure dissCalcfunction = new DissimilarityMeasure(dissimilarityMatrix);
            for (double[] prototype : prototypeValues) {
                dist = dissCalcfunction.calculateDistance(prototype, tempExampleValues);
                double protoLabel = prototypeLabels[i];
                if (dist < minDistCorrect && tempExampleLabel == protoLabel) {
                    minDistCorrect = dist;
                }
                if (dist < minDistIncorrect && tempExampleLabel != protoLabel) {
                    minDistIncorrect = dist;
                }
                i++;
            }

            double denominator = minDistCorrect + minDistIncorrect;
            denominator = denominator == 0 ? 1e-10 : denominator;
            double mu = (minDistCorrect - minDistIncorrect) / denominator;
            costValue += 1 / (1+Math.exp(-mu * time));
        }
        return costValue / trainingSet.size();
    }*/

    /**
     * Returns total number of iterations (maximum number of iterations)
     *
     * @return
     */
    @Override
    public int getMaxIterations() {
        return iterations;
    }

    /**
     * Returns current iteration
     *
     * @return
     */
    @Override
    public int getIteration() {
        return currentIteration;
    }

    /**
     * Returns the value of cost function
     *
     * @return
     */
    @Override
    public double getCostFunctionValue() {
        if (costValues.size()>0)
            return costValues.get(costValues.size()-1);
        return -1;
    }

    /**
     * Returns list of cost function values
     *
     * @return
     */
    @Override
    public List<Double> getCostFunctionValues() {
        return costValues;
    }





}

