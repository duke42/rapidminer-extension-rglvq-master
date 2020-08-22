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
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DissimilarityMeasure{ //extends DissMatrix{

    private double[][] prototypeValues;
    private static double [][] dissimilarityMatrix;
    //private int attributesSize = 0;
    List<Attribute> trainingAttributes;

    public DissimilarityMeasure(double[][]dissimilarityMatrix) {

        this.dissimilarityMatrix = dissimilarityMatrix;
        //this.attributesSize = dissimilarityMatrix.length;

    }



    public double calculateDistance(double[] prototypes, double[] exampleValues) {

        //z
        double[] matrixVector = new double[dissimilarityMatrix.length];//matrixvector[k].length = numberOfPrototypes
        double z = 0;
        double product = 0;
        int i;
        int j;
        int k = 0;
            //D * α_j
        for (i = 0; i < dissimilarityMatrix.length; i++) {
            for (j = 0; j < dissimilarityMatrix[0].length; j++) {

                matrixVector[k] += dissimilarityMatrix[i][j] * prototypes[j];



                //LogService.getRoot().log(Level.INFO, "DisMeasure: dissMatrix: " + dissimilarityMatrix[i][j]);
                //LogService.getRoot().log(Level.INFO, "DisMeasure: prototype: " + prototypes[j]);
                //LogService.getRoot().log(Level.INFO, "DisMeasure: matrixVector[k]"+matrixVector[k]);
                //LogService.getRoot().log(Level.INFO, "DisMeasure: i"+i);
                //LogService.getRoot().log(Level.INFO, "DisMeasure: j"+j);
                //LogService.getRoot().log(Level.INFO, "DisMeasure: k"+k);
            }
            k++;
        }
        //Dα_j * α_j
        for (i = 0; i < matrixVector.length; i++) {
            product += matrixVector[i] * prototypes[i];
        }
        z = (-0.5) * product;
        //LogService.getRoot().log(Level.INFO, "DisMeasure: z"+z);

        //A
        i = 0;
        double productA = 0; //α_j * D
        double distance = 0;
        for (double e : exampleValues) {

                productA += e * prototypes[i];
            //LogService.getRoot().log(Level.INFO, "DisMeasure: exampleValue"+e);
                i++;
        }

        //distance = A - z
        distance = productA + z;
        //LogService.getRoot().log(Level.INFO, "Ducanh: distance DisMeasure: " + distance);

        return distance;


    }

   /* @Override
    public double calculateSimilarity(double[] value1, double[] value2) {
        return -calculateDistance(value1, value2);
    }*/




}








