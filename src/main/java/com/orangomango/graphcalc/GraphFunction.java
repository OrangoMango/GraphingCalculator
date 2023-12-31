package com.orangomango.graphcalc;

import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.util.*;
import java.util.function.Function;

public class GraphFunction{
	private Function<Double, Double> func;
	private List<Result> results = new ArrayList<>();
	private Color color;
	private String expression = null;
	private String a, b;
	private boolean quadratic = false;

	public GraphFunction(Color color, String f){
		this.color = color;
		this.expression = f.replace(" ", ""); // TODO
		this.func = Evaluator.buildFunction(this.expression.split("=")[1].trim(), "x", null);

		// Get the coefficients
		String[] parts = this.expression.split("=")[0].replace("-", "+-").split("\\+");
		for (String p : parts){
			if (p.contains("y^2")){
				this.quadratic = true;
				this.a = p.replace("*y^2", "").replace("y^2*", "").replace("y^2", "");
			} else if (p.contains("y")){
				this.b = p.replace("*y", "").replace("y*", "").replace("y", "");
			}
		}
		System.out.format("A: %s B: %s\n", this.a, this.b);
	}

	public static void addFunction(List<GraphFunction> list, GraphFunction f){
		f.buildInterval(-10, 10, 0.005, -10, 10);
		list.add(f);
	}

	public static void removeFunction(List<GraphFunction> list, GraphFunction f){
		list.remove(f);
	}

	public void buildInterval(double from, double to, double step, double minY, double maxY){
		List<Pair<Double, Double>> values1 = new ArrayList<>();
		List<Pair<Double, Double>> values2 = new ArrayList<>();
		for (double i = from; i <= to; i += step){
			List<Double> output = solveForY(i);
			values1.add(new Pair<Double, Double>(i, output.get(0) > minY && output.get(0) < maxY ? output.get(0) : null));
			if (output.size() > 1) values2.add(new Pair<Double, Double>(i, output.get(1) > minY && output.get(1) < maxY ? output.get(1) : null));
		}

		this.results.add(new Result(from, to, step, values1));
		if (this.quadratic){
			this.results.add(new Result(from, to, step, values2));
		}
	}

	public List<Double> solveForY(double x){
		Map<String, Double> param = new HashMap<>();
		param.put("x", x);
		double a, b;

		if (this.a == null) a = 0;
		else if (this.a.equals("")) a = 1;
		else {
			Evaluator eval = new Evaluator(this.a, param);
			a = eval.parse();
		}
		if (this.b == null) b = 0;
		else if (this.b.equals("")) b = 1;
		else {
			Evaluator eval = new Evaluator(this.b, param);
			b = eval.parse();
		}

		double c = -func.apply((int)(x*1000)/1000.0);
		List<Double> output = new ArrayList<>();

		if (a == 0){
			output.add(-c/b);
		} else {
			double y1 = (-b+Math.sqrt(b*b-4*a*c))/(2*a);
			double y2 = (-b-Math.sqrt(b*b-4*a*c))/(2*a);
			output.add(y1);
			output.add(y2);
		}

		return output;
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
		return this.expression;
	}
}