package com.orangomango.graphcalc;

import javafx.util.Pair;

import java.util.*;

public class Result{
	private List<Pair<Double, Double>> value;
	private double from, to;

	public Result(double from, double to, List<Pair<Double, Double>> value){
		this.from = from;
		this.to = to;
		this.value = value;
	}

	public List<Pair<Double, Double>> getValues(){
		return this.value;
	}

	public void setFrom(double value){
		this.from = value;
	}

	public void setTo(double value){
		this.to = value;
	}

	public double getFrom(){
		return this.from;
	}

	public double getTo(){
		return this.to;
	}
}