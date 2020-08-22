package com.rapidminer.rglvq.operator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.LogService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public abstract class AbstractModel{

    ExampleSet prototypes;
    private Example example; //Current example from the example set
    double[] exampleValues; //double values retrived from the example set
    double exampleLabel; //Current label
    Attributes prototypeAttributes; //List of attributes
    List<Attribute> trainingAttributes;
    private final int attributesSize;
    private final int numberOfPrototypes; //Number of prototypes and number of attributes
    double[][] prototypeValues;
    double[] prototypeLabels;
    Map<String, Object> storedValues;
    double[][] squaredDissMatrix;
    public double COEFF_CUTOFF = 1E-3;
    //double [] labelList = new double [dissimilarityMatrix.length];

    public AbstractModel(ExampleSet prototypes) {

        prototypeAttributes = prototypes.getAttributes();
        this.prototypes = prototypes;
        attributesSize = prototypeAttributes.size();
        numberOfPrototypes = prototypes.size();
        storedValues = new HashMap<>();

    }

    /**
     * This method draws single Examples from the ExampleSet and calls the RGLVQ update Method
     * @param trainingSet - training ExampleSet
     *
     * @return - optimized Prototypes
     */

    public ExampleSet run(ExampleSet trainingSet, double [] labelList) {
        Attributes tempTrainingAttributes = trainingSet.getAttributes();
        trainingAttributes = new ArrayList<Attribute>(tempTrainingAttributes.size());

        int i = 0;
        int j = 0;

        //Caching codebooks for faster optimization
        prototypeValues = new double[numberOfPrototypes][attributesSize];
        prototypeLabels = new double[numberOfPrototypes];
        for (Example p : prototypes) {
            j = 0;
            for (Attribute a : prototypeAttributes) {
                prototypeValues[i][j] = p.getValue(a);
                if(prototypeValues[i][j] == 0) {
                    prototypeValues[i][j] = Math.random() + 0.1;
                }
                j++;
            }
            prototypeLabels[i] = p.getLabel();
            i++;
        }
        /*for(int z = 0; z < prototypeValues.length; z++) {
            for (int x = 0; x < prototypeValues[0].length; x++) {
                LogService.getRoot().log(Level.INFO, "Ducanh: prototypeValues withoutZero: " + prototypeValues[z][x]);
            }
        }
        for(int t = 0; t < labelList.length; t++) {
            LogService.getRoot().log(Level.INFO, "Ducanh: labelList " + labelList[t]);
        }*/
        double [] uniqueLabels = Arrays.stream(prototypeLabels).distinct().toArray();
        //set sparse representation of prototypes
        for(i = 0; i < prototypeLabels.length; i++) {
            for (j = 0; j < squaredDissMatrix[0].length; j++) {
                if (prototypeLabels[i] != labelList[j]) {
                    prototypeValues[i][j] = 0;
                }
                //LogService.getRoot().log(Level.INFO, "Ducanh: sparsePrototypes: " + prototypeValues[i][j]);
            }
        }

        //Normalize to 1
        double[] prototypeSum = new double[prototypeValues.length];
        int k = 0;
        int l = 0;
        int m = 0;
        double a = 0;

        for (i = 0; i < prototypeValues.length; i++){
            for (j = 0; j < prototypeValues[0].length; j++) {
                prototypeSum[i] += prototypeValues[i][j];
            }
            //k++;
        }
        for (i = 0; i < prototypeValues.length; i++) {
            for (j = 0; j < prototypeValues[0].length; j++) {
                prototypeValues[i][j] = prototypeValues[i][j] / prototypeSum[i];
                //LogService.getRoot().log(Level.INFO, "Ducanh: NormalizedPrototypes: " + prototypeValues[i][j]);
            }
        }

        //Reordering attributes
        for (Attribute b : prototypeAttributes) {
            trainingAttributes.add(tempTrainingAttributes.get(b.getName()));
        }

        //fetch single rows from ExampleSet and call update Method
        exampleValues = new double[prototypeAttributes.size()];
        do {
            for (Example trainingExample : trainingSet) {
                this.example = trainingExample;
                i = 0;
                for (Attribute attribute : trainingAttributes) {
                    exampleValues[i] = trainingExample.getValue(attribute);
                    i++;
                }
                exampleLabel = trainingExample.getLabel();
                update();
            }
            //set values to zero
            for(int o = 0; o < prototypeValues.length; o++) {
                for (int p = 0; p < prototypeValues[0].length; p++) {

                    if (prototypeValues[o][p] < 0) {
                        prototypeValues[o][p] = 0;

                    }
                }
            }
            //Normalize to 1
            prototypeSum = new double[prototypeValues.length];
            k = 0;
            a = 0;
            for ( i = 0; i < prototypeValues.length; i++){
                for (j = 0; j < prototypeValues[0].length; j++) {
                    prototypeSum[i] += prototypeValues[i][j];
                }
                //k++;
            }
            for (i = 0; i < prototypeValues.length; i++) {
                for (j = 0; j < prototypeValues[0].length; j++) {
                    prototypeValues[i][j] = prototypeValues[i][j] / prototypeSum[i];
                    //LogService.getRoot().log(Level.INFO, "Ducanh: NormalizedPrototypes: " + prototypeValues[i][j]);
                }
            }
        } while (nextIteration(trainingSet));

        i = 0;
        for (Example p : prototypes) {
            j = 0;
            for (Attribute b : prototypeAttributes) {
                p.setValue(b, prototypeValues[i][j]);
                j++;
            }
            i++;
        }
        return prototypes;
    }


    /**
     * Returns total number of iterations (maximum number of iterations)
     *
     * @return
     */
    abstract public int getMaxIterations();

    /**
     * Returns current iteration
     *
     * @return
     */
    abstract public int getIteration();

    /**
     * Returns the value of cost function
     *
     * @return
     */
    abstract public double getCostFunctionValue();

    /**
     * Returns list of cost function values
     *
     * @return
     */
    abstract public List<Double> getCostFunctionValues();

    /**
     *
     * @return
     */
    protected Example getCurrentExample() {
        return example;
    }

    /**
     *
     * @return
     */
    protected double[] getCurrentExampleValues() {
        return exampleValues;
    }

    /**
     *
     * @return
     */
    protected int getAttributesSize() {
        return attributesSize;
    }

    /**
     *
     * @return
     */
    protected int getNumberOfPrototypes() {
        return numberOfPrototypes;
    }

    /**
     *
     * @param i
     * @return
     */
    protected double[] getPrototypeValues(int i) {
        return prototypeValues[i];
    }

    /**
     *
     * @param i
     * @return
     */
    protected double getPrototypeLabel(int i) {
        return prototypeLabels[i];
    }

    /**
     *
     * @return
     */
    abstract boolean nextIteration(ExampleSet trainingSet);

    /**
     * Method executed before the training starts.
     * @param trainingSet
     */
    public void beforeTraining(ExampleSet trainingSet){
    }

    /**
     * Method executed then the main loop of the LVQ algorithm is finished.
     * @param trainingSet
     */
    public void afterTraining(ExampleSet trainingSet){
    }

    /**
     *
     */
    abstract void update();

    public final Object getStoredValue(String key) {
        return storedValues.get(key);
    }

    public final void addStoredValue(String key, Object value){
        storedValues.put(key,value);
    }




    }






