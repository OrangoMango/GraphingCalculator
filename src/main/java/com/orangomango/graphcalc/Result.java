package com.orangomango.graphcalc;

import javafx.util.Pair;

import java.util.*;

public class Result{
	private List<Pair<Double, Double>> value;
	private double from, to, step;

	public Result(double from, double to, double step, List<Pair<Double, Double>> value){
		this.from = from;
		this.to = to;
		this.step = step;
		this.value = value;
	}

	public List<Pair<Double, Double>> getValue(){
		return this.value;
	}
}