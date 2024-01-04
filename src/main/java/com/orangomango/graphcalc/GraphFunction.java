package com.orangomango.graphcalc;

import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.util.*;
import java.util.function.Function;

import com.orangomango.graphcalc.math.*;

public class GraphFunction{
	private Function<Double, Double> func;
	private List<Result> results = new ArrayList<>();
	private Color color;
	private Equation equation;
	private String a, b;
	private boolean quadratic = false;

	public GraphFunction(Color color, String f){
		this.color = color;
		this.equation = new Equation(f);
		this.quadratic = this.equation.getEquation().contains("y^2");
		//this.equation.moveAllTerms(term -> term.getTerm().contains("y"), true);
		//this.equation.moveAllTerms(term -> !term.getTerm().contains("y"), false);
		//this.func = Evaluator.buildFunction(this.equation.getEquation().split("=")[1], "x", null);
		//this.equation.moveAllTerms(term -> !term.getLeft(), true);
	}

	public static void addFunction(List<GraphFunction> list, GraphFunction f){
		f.buildInterval(-10, 10, 0.005, -10, 10);
		list.add(f);
	}

	public static void removeFunction(List<GraphFunction> list, GraphFunction f){
		list.remove(f);
	}

	public GraphFunction transform(Color color, String xEq, String yEq){
		xEq = xEq.replace(" ", "").split("=")[1].replace("x'", "#").replace("y'", "@");
		yEq = yEq.replace(" ", "").split("=")[1].replace("x'", "#").replace("y'", "@");
		System.out.println("From: "+this.equation.getEquation());
		String eq = this.equation.getEquation().replace("x", "("+xEq+")").replace("y", "("+yEq+")").replace("#", "x").replace("@", "y");
		System.out.println("eq: "+eq);
		Equation equation = new Equation(eq);
		GraphFunction f = new GraphFunction(color, equation.getEquation());
		return f;
	}

	public void buildInterval(double from, double to, double step, double minY, double maxY){
		List<Pair<Double, Double>> values1 = new ArrayList<>();
		List<Pair<Double, Double>> values2 = new ArrayList<>();
		for (double i = from; i <= to; i += step){
			Map<String, Double> params = new HashMap<>();
			params.put("x", i);
			List<Double> output = this.equation.solve("y", params);
			values1.add(new Pair<Double, Double>(i, output.get(0) > minY && output.get(0) < maxY ? output.get(0) : null));
			if (output.size() > 1) values2.add(new Pair<Double, Double>(i, output.get(1) > minY && output.get(1) < maxY ? output.get(1) : null));
		}

		this.results.add(new Result(from, to, step, values1));
		if (this.quadratic){
			this.results.add(new Result(from, to, step, values2));
		}
	}

	public Function<Double, Double> getDefinition(){
		return this.func;
	}

	public Color getColor(){
		return this.color;
	}

	public List<Result> getResults(){
		return this.results;
	}

	public boolean isQuadratic(){
		return this.quadratic;
	}

	@Override
	public String toString(){
		return this.equation.getEquation();
	}
}