package com.orangomango.graphcalc;

import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.util.*;

import com.orangomango.graphcalc.math.*;

public class GraphFunction{
	private List<Result> results = new ArrayList<>();
	private Color color;
	private Equation equation;
	private String a, b;
	private boolean quadratic = false;

	public static final double FUNCTION_INTERVAL = 0.005;

	public GraphFunction(Color color, String f){
		this.color = color;
		this.equation = new Equation(f);
		this.quadratic = this.equation.getEquation().contains("y^2");
	}

	public static void addFunction(List<GraphFunction> list, GraphFunction f, double lp, double rp, Map<String, Double> params){
		f.buildInterval(lp, rp, params);
		list.add(f);
	}

	public static void removeFunction(List<GraphFunction> list, GraphFunction f){
		list.remove(f);
	}

	public GraphFunction transform(Color color, String xEq, String yEq){
		xEq = xEq.replace(" ", "").replace("x", "#").replace("y", "@");
		yEq = yEq.replace(" ", "").replace("x", "#").replace("y", "@");
		String eq = this.equation.getEquation().replace("x", "("+xEq+")").replace("y", "("+yEq+")").replace("#", "x").replace("@", "y");
		Equation equation = new Equation(eq);
		equation.getLeftSide().calculate(null);
		GraphFunction f = new GraphFunction(color, equation.getEquation());
		return f;
	}

	private void buildInterval(double from, double to, Map<String, Double> args){
		List<Pair<Double, Double>> values1 = new ArrayList<>();
		List<Pair<Double, Double>> values2 = new ArrayList<>();

		Expression expression = (Expression)this.equation.getLeftSide().copy(null);
		Expression rightCopy = (Expression)this.equation.getRightSide().copy(null);
		Equation.prepareForSolving(expression, rightCopy, "y", args);

		for (double i = from; i <= to; i += FUNCTION_INTERVAL){
			Map<String, Double> params = new HashMap<>(args);
			params.put("x", i);
			List<Double> output = Equation.solve(expression, rightCopy, "y", params);

			// y1
			if (Double.isFinite(output.get(0))){
				values1.add(new Pair<Double, Double>(i, output.get(0)));
			} else {
				values1.add(new Pair<Double, Double>(i, output.get(0).isInfinite() ? output.get(0) : null));
			}

			// y2
			if (output.size() > 1 && Double.isFinite(output.get(1))){
				values2.add(new Pair<Double, Double>(i, output.get(1)));
			} else {
				values2.add(new Pair<Double, Double>(i, null));
			}
		}

		this.results.add(new Result(from, to, values1));
		if (this.quadratic){
			this.results.add(new Result(from, to, values2));
		}
	}

	public void expand(double from, double to, Map<String, Double> args){
		boolean firstDone = false;
		for (Result result : this.results){
			if (Math.abs(result.getFrom()-from) < FUNCTION_INTERVAL && Math.abs(to-result.getTo()) < FUNCTION_INTERVAL){
				firstDone = true;
				continue;
			}

			int startIndex = -1;
			int endIndex = -1;
			for (int i = 0; i < result.getValues().size(); i++){
				Pair<Double, Double> pair = result.getValues().get(i);
				if (pair.getKey() < from){
					startIndex = i;
				}
				if (pair.getKey() > to && endIndex == -1){
					endIndex = i;
				}
			}

			if (startIndex != -1) result.getValues().subList(0, startIndex).clear();
			if (endIndex != -1) result.getValues().subList(endIndex, result.getValues().size()).clear();
			Expression expression = (Expression)this.equation.getLeftSide().copy(null);
			Expression rightCopy = (Expression)this.equation.getRightSide().copy(null);
			Equation.prepareForSolving(expression, rightCopy, "y", args);

			// LEFT
			for (double i = result.getFrom()-FUNCTION_INTERVAL; i >= from; i -= FUNCTION_INTERVAL){
				Map<String, Double> params = new HashMap<>(args);
				params.put("x", i);
				Double solution = Equation.solve(expression, rightCopy, "y", params).get(firstDone ? 1 : 0);

				if (Double.isFinite(solution)){
					result.getValues().add(0, new Pair<Double, Double>(i, solution));
				} else {
					result.getValues().add(0, new Pair<Double, Double>(i, solution.isInfinite() ? solution : null));
				}
			}

			// RIGHT
			for (double i = result.getTo()+FUNCTION_INTERVAL; i <= to; i += FUNCTION_INTERVAL){
				Map<String, Double> params = new HashMap<>(args);
				params.put("x", i);
				Double solution = Equation.solve(expression, rightCopy, "y", params).get(firstDone ? 1 : 0);

				if (Double.isFinite(solution)){
					result.getValues().add(new Pair<Double, Double>(i, solution));
				} else {
					result.getValues().add(new Pair<Double, Double>(i, solution.isInfinite() ? solution : null));
				}
			}

			result.setFrom(from);
			result.setTo(to);

			firstDone = true;
		}
	}

	public Equation getEquation(){
		return this.equation;
	}

	public Color getColor(){
		return this.color;
	}

	public void setColor(Color color){
		this.color = color;
	}

	public List<Result> getResults(){
		return this.results;
	}

	public boolean isQuadratic(){
		return this.quadratic;
	}

	@Override
	public String toString(){
		return this.equation.toString();
	}
}