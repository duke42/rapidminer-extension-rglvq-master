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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.GeneratePredictionModelTransformationRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RandomGenerator;

public class RGLVQOperator extends Operator implements CapabilityProvider {

    /*//Atributte for Debugging
    boolean debugMode = false;
    static Attributes debugAtt;
    */

    private InputPort exampleSetInputPort = getInputPorts().createPort("validation example set");
    private InputPort exampleSetInputPortOriginal = getInputPorts().createPort("original example set");

    //private OutputPort exampleSetOutputPort = getOutputPorts().createPassThroughPort("example set");
    private OutputPort exampleSetOutputPort = getOutputPorts().createPort("example set");
    private OutputPort modelOutputPort = getOutputPorts().createPort("model");
    private OutputPort prototypesOutputPort = getOutputPorts().createPort("prototypes");
    private OutputPort costProgressOutputPort = getOutputPorts().createPort("cost progress");


    /**
     * Parameter for PPC
     */
    public static final String PARAMETER_PROTOTYPES_PER_CLASS = "Prototypes per class"; //K
    /**
     * Parameter for Iterations
     */
    public static final String PARAMETER_ITERATIONS = "Iterations"; //T
    /**
     * Parameter for LearningRate
     */
    public static final String PARAMETER_LEARNING_RATE = "Learning rate: Alpha";
    /**
     * Parameter for error Log
     */
    public static final String PARAMETER_COST_LOG = "error log";


    private int ppc; //K
    private int numberOfIterations; //T
    private double initialLearningRate; //α
    private boolean costLog;
    ExampleSet initialPrototypes;
    ExampleSet optimizedPrototypes;
    List<Attribute> trainingAttributes;
    List<Attribute> trainingAttributesOriginal;
    double[][] dissMatrix;
    double[][] squaredDissMatrix;
    double[][]dissMatrixOriginal;

    /**
     * Constructor - gets called, as soon the operator gets dragged into process panel
     *
     * @param description
     */
    public RGLVQOperator(OperatorDescription description) {
        super(description);

        //Preconditions training set input port
        exampleSetInputPort.addPrecondition(new CapabilityPrecondition(this, exampleSetInputPort));
        exampleSetInputPort.addPrecondition(new ExampleSetPrecondition(exampleSetInputPort, "label", 0));
        exampleSetInputPort.addPrecondition(new CapabilityPrecondition(new CapabilityProvider() {

            @Override
            public boolean supportsCapability(OperatorCapability capability) {
                switch (capability) {
                    case POLYNOMINAL_ATTRIBUTES:
                    case NUMERICAL_ATTRIBUTES:
                    case POLYNOMINAL_LABEL:
                    case BINOMINAL_LABEL:
                    case NUMERICAL_LABEL:
                        return true;
                    default:
                        return false;
                }

            }
        }, exampleSetInputPort));

        //Transformation Rules for Metadata
        getTransformer().addPassThroughRule(exampleSetInputPort, exampleSetOutputPort);
        getTransformer().addRule(new GeneratePredictionModelTransformationRule(exampleSetInputPort, modelOutputPort, RGLVQClassificationModel.class));
        addPrototypeTransformationRule();

        /**
         * default values Hyperparameters
         */
        ppc = 1;
        numberOfIterations = ppc * 50;
        initialLearningRate = 0.1;
        costLog = false;
    }

    /**
     * Main method
     *
     * @return
     */
    @Override
    public void doWork() throws OperatorException {

        //fetch data from InputPort and deliver it to the Outputport
        ExampleSet trainingSet = exampleSetInputPort.getData(ExampleSet.class);
        ExampleSet trainingSetOriginal = exampleSetInputPortOriginal.getData(ExampleSet.class);

        //Caching Matrix
        Attributes tempTrainingAttributes = trainingSet.getAttributes();
        trainingAttributes = new ArrayList<Attribute>(tempTrainingAttributes.size());
        dissMatrix = new double[trainingSet.size()][tempTrainingAttributes.size()];//
        squaredDissMatrix = new double[trainingSet.size()][tempTrainingAttributes.size()];//
        int i = 0;
        int j = 0;

        for (Example p : trainingSet) {
            j = 0;
            for (Attribute a : tempTrainingAttributes) {
                dissMatrix[i][j] = p.getValue(a);
                squaredDissMatrix[i][j] = Math.pow(dissMatrix[i][j], 2);
                //LogService.getRoot().log(Level.INFO, "Operator: dissMatrix doWork: " + squaredDissMatrix[i][j]);
                //LogService.getRoot().log(Level.INFO, "Operator: i : " + i);
                //LogService.getRoot().log(Level.INFO, "Operator: j  " + j);
                j++;
            }
            i++;
        }

        //getLabelindex
        Attributes tempTrainingAttributesOriginal = trainingSetOriginal.getAttributes();
        trainingAttributesOriginal = new ArrayList<Attribute>(tempTrainingAttributesOriginal.size());
        dissMatrixOriginal = new double[trainingSetOriginal.size()][tempTrainingAttributesOriginal.size()];
        i = 0;
        j = 0;
        double [] labelList = new double [dissMatrixOriginal.length];

        for(Example q : trainingSetOriginal){
            j=0;
            for (Attribute b : tempTrainingAttributesOriginal) {
                labelList[i] = q.getLabel();
            }
            i++;
        }

        //get selected Hyperparameters
        ppc = getParameterAsInt(PARAMETER_PROTOTYPES_PER_CLASS);
        numberOfIterations = getParameterAsInt(PARAMETER_ITERATIONS); //T
        initialLearningRate = getParameterAsDouble(PARAMETER_LEARNING_RATE); //α
        costLog = getParameterAsBoolean(PARAMETER_COST_LOG);

        //initialize Prototypes
        initialPrototypes = initializeCodebooks(trainingSet, ppc);

        //instantiate RGLVQModel
        AbstractModel rglvqModel = new RGLVQModel(initialPrototypes, numberOfIterations, initialLearningRate, squaredDissMatrix);
        optimizedPrototypes = rglvqModel.run(trainingSet, labelList);

        RGLVQClassificationModel<Double> classificationModel = new RGLVQClassificationModel<Double>(optimizedPrototypes, trainingSet, squaredDissMatrix );
        PredictionModel rapidMinerModel = classificationModel;
        exampleSetOutputPort.deliver(trainingSet);
        modelOutputPort.deliver(rapidMinerModel);
        prototypesOutputPort.deliver(optimizedPrototypes);
    }

    /**
     * Main method responsible for processing input example set
     * @param numberOfPrototypesPerClass - numberOfPrototypes
     * @param trainingSet - training ExampleSet
     *@return - exampleSet of cluster centers
     *@throws OperatorException - operator Exception
     */
    public ExampleSet initializeCodebooks(ExampleSet trainingSet, int numberOfPrototypesPerClass) throws OperatorException{

        //Default number of prototypes per class
        ExampleSet codebooks = null;
        if (numberOfPrototypesPerClass < 1) {
            numberOfPrototypesPerClass = trainingSet.size() / 10;
        }

        RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);

        //fetch Attributes from ExampleSet
        Iterator<Attribute> attributes = trainingSet.getAttributes().allAttributes();

        //Caching Attributes
        ArrayList<Attribute> attributesList = new ArrayList<>();
        for (Iterator<Attribute> i = attributes; attributes.hasNext();) {
            attributesList.add((Attribute) i.next().clone());
        }

        Attribute[] attributesArr = attributesList.toArray(new Attribute[attributesList.size()]);
        String[] attributeStr = new String[attributesList.size()];
        int attrI = 0;
        for (Attribute attr : attributesArr) {
            attributeStr[attrI] = attr.getName();
            attrI++;
        }
        //attributes =  trainingSet.getAttributes().allAttributes();??
        Attribute label = trainingSet.getAttributes().getLabel();
        //LogService.getRoot().log(Level.INFO, "Ducanh: Attribute label: " + label);
            /*//check if lable is set
            if(label != null) {
                if(debugMode) LogService.getRoot().log(Level.INFO, "JAN:            Label =  " + label.getName());
            }
            else {
                throw new OperatorException("Could not identify the label inside the input data. Assign the role for your classification attribute.");
            }
            */

        //get distinct classLabels
        HashSet<String> classes = new HashSet<>();
        Iterator<Example> trainingSetIteration = trainingSet.iterator();
        for (Iterator<Example> i = trainingSetIteration; i.hasNext();) {
            classes.add((String) i.next().getValueAsString(label));
        }

        //initialize Prototypes
        List<Attribute> listOfAtts = new LinkedList<>();
        boolean lbl = false;
        Attribute cbLbl = null;

        for (Attribute att : attributesList) {
            Attribute newAtt = AttributeFactory.createAttribute(att);
            if (att.getName() == trainingSet.getAttributes().getLabel().getName()) lbl = true;
            if (lbl) cbLbl = newAtt;
            lbl = false;
            listOfAtts.add(newAtt);
        }

        // ExampleSetBuilder wird f�r die Erzeugung neuer ExampleSets ben�tigt
        ExampleSetBuilder builder = ExampleSets.from(listOfAtts);
        builder.withRole(cbLbl, "label");

        // Examples werden mit double Arrays erzeugt
        double[] doubleArray = new double[attributesList.size()];

        //fetching random Examples from input data
        for (String cls : classes) {
            //if(debugMode)
            int i = 1;

            Example randomExample = null;

            for (randomExample = trainingSet.getExample(randomGenerator.nextInt(trainingSet.size() - 1)); i <= numberOfPrototypesPerClass; randomExample = trainingSet.getExample(randomGenerator.nextInt(trainingSet.size() - 1))) {

                if (randomExample.getValueAsString(label) == cls) {
                    int arrIndex = 0;

                    for (String att : attributeStr) {
                        doubleArray[arrIndex] = randomExample.getValue(randomExample.getAttributes().get(att));
                        arrIndex++;
                    }
                    //if(debugMode)
                    builder.addRow(doubleArray);
                    i++;
                }
            }
        }
        //generate prototypes
        codebooks = builder.build();
        /*LogService.getRoot().log(Level.INFO, "INITIAL PROTOTYPES");
        for(Example example : codebooks) {
            printExample(example);
        }*/
        return codebooks;
    }

        /**
         *
         */
        protected MetaData modifyPrototypeOutputMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
            try {
                metaData.setNumberOfExamples(getNumberOfPrototypesMetaData());
            } catch (UndefinedParameterError e) {
                metaData.setNumberOfExamples(new MDInteger());
            }
            return metaData;
        }

        /**
         * It returns number of proptotypes in the ExampleSetMetaData returned by
         * the prototypeOutput
         *
         * @return
         * @throws UndefinedParameterError
         */
        protected MDInteger getNumberOfPrototypesMetaData() throws UndefinedParameterError {
            int num = getParameterAsInt(PARAMETER_PROTOTYPES_PER_CLASS);
            return new MDInteger(num);
        }

        // Capability Provider
        @Override
        public boolean supportsCapability(OperatorCapability capability){
            switch (capability) {
                case POLYNOMINAL_ATTRIBUTES:
                case NUMERICAL_ATTRIBUTES:
                case POLYNOMINAL_LABEL:
                case BINOMINAL_LABEL:
                case NUMERICAL_LABEL:
                    return true;
                default:
                    return false;
            }
        }

        //Adding Operator Parameters
        @Override
        public List<ParameterType> getParameterTypes() {
            List<ParameterType> types = super.getParameterTypes();

            ParameterType type;

            type = new ParameterTypeInt(PARAMETER_ITERATIONS, "Number of Iterations", 1, Integer.MAX_VALUE, this.numberOfIterations);
            type.setExpert(false);
            types.add(type);

            type = new ParameterTypeDouble(PARAMETER_LEARNING_RATE, "Learning Rate", 0.0000000001, 0.9999999999, this.initialLearningRate);
            type.setExpert(false);
            types.add(type);

            type = new ParameterTypeInt(PARAMETER_PROTOTYPES_PER_CLASS, "Number of prototypes per class", 1, Integer.MAX_VALUE, this.ppc);
            type.setExpert(false);
            types.add(type);

            type = new ParameterTypeBoolean(PARAMETER_COST_LOG, "Show Error for each epoch on Log-Screen", false, this.costLog);
            type.setExpert(false);
            types.add(type);

            return types;
        }

    /**
     * This method generates PrototypeOutput metadata.
     */
    protected void addPrototypeTransformationRule() {
        getTransformer().addRule(new PassThroughRule(exampleSetInputPort, prototypesOutputPort, true) {
            @Override
            public MetaData modifyMetaData(MetaData metaData) {
                if (metaData instanceof ExampleSetMetaData) {
                    try {
                        return RGLVQOperator.this.modifyPrototypeOutputMetaData((ExampleSetMetaData) metaData);
                    } catch (UndefinedParameterError ex) {
                        return metaData;
                    }
                } else {
                    return metaData;
                }
            }

        });
    }

    /**
     *
     * @param example - example
     */
    public static void printExample(Example example) {
        String out = new String();
        ArrayList<String> attNames = new ArrayList<>();
        for (Iterator<Attribute> debugAtt = example.getAttributes().allAttributes(); debugAtt.hasNext();) {
            attNames.add(debugAtt.next().getName());
        }
        for (String name : attNames) {
            out += name + ": " + example.getValue(example.getAttributes().get(name)) + "  -  ";
        }
        LogService.getRoot().log(Level.INFO, "example: " + out + "typeOfDataRow" + example.getDataRow().getType());
    }

}