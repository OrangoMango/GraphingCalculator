package com.orangomango.graphcalc;

import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.util.*;
import java.util.function.Function;

public class GraphFunction{
	private Function<Double, Double> func;
	private Result result;
	private Color color;

	public GraphFunction(Color color, Function<Double, Double> f){
		this.color = color;
		this.func = f;
	}

	public Function<Double, Double> getDefinition(){
		return this.func;
	}

	public Color getColor(){
		return this.color;
	}

	public void clearResult(){
		this.result = null;
	}

	public Result getResult(){
		return this.result;
	}

	public void buildInterval(double from, double to, double step){
		List<Pair<Double, Double>> value = new ArrayList<>();
		for (double i = from; i <= to; i += step){
			value.add(new Pair<Double, Double>(i, func.apply(i)));
		}

		this.result = new Result(from, to, step, value);
	}
}