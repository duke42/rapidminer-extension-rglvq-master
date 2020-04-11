package com.rapidminer.rglvq.operator;

import java.util.logging.Level;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.LogService;

import java.util.logging.Level;

public class RGLVQOperator extends Operator {




    //Constructor
    public RGLVQOperator (OperatorDescription description) {
        super(description);
    }

    @Override
    public void doWork() throws OperatorException {
        LogService.getRoot().log(Level.INFO, "Doing something...");
    }









}
