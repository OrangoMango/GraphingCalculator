package com.orangomango.graphcalc;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.util.*;

import com.orangomango.graphcalc.math.*;

public class GraphFunction extends GraphElement implements Transformable{
	private List<Result> results = new ArrayList<>();
	private Equation equation;
	private String a, b;
	private boolean quadratic = false;

	public static final double FUNCTION_INTERVAL = 0.005;

	public GraphFunction(Color color, String f){
		super(color);
		this.equation = new Equation(f);
		this.quadratic = this.equation.getEquation().contains("y^2");
	}

	public static void addFunction(List<GraphElement> list, GraphFunction f, double lp, double rp, Map<String, Double> params){
		f.buildInterval(lp, rp, params);
		list.add(f);
	}

	@Override
	public GraphElement transform(Color color, String xEq, String yEq){
		xEq = xEq.replace(" ", "").replace("x", "#").replace("y", "@");
		yEq = yEq.replace(" ", "").replace("x", "#").replace("y", "@");
		String eq = this.equation.getEquation().replace("x", "("+xEq+")").replace("y", "("+yEq+")").replace("#", "x").replace("@", "y");
		Equation equation = new Equation(eq);
		equation.getLeftSide().calculate(null);
		GraphFunction f = new GraphFunction(color, equation.getEquation());
		return f;
	}

	@Override
	public void edit(String f, Map<String, Double> params){
		this.equation = new Equation(f);
		this.quadratic = this.equation.getEquation().contains("y^2");
		double from = this.results.get(0).getFrom();
		double to = this.results.get(0).getTo();
		this.results.clear();
		buildInterval(from, to, params);
	}

	private void buildInterval(double from, double to, Map<String, Double> args){
		// Fix the bounds
		from = from < 0 ? Math.floor(from) : Math.ceil(from);
		to = to < 0 ? Math.floor(to) : Math.ceil(to);

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
		// Fix the bounds
		from = from < 0 ? Math.floor(from) : Math.ceil(from);
		to = to < 0 ? Math.floor(to) : Math.ceil(to);

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
			if (endIndex != -1) result.getValues().subList(startIndex != -1 ? endIndex-startIndex : endIndex, result.getValues().size()).clear();
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

	@Override
	public void render(GraphicsContext gc, double topPos, double bottomPos, double scaleFactor){
		gc.setStroke(this.color);
		for (Result rs : this.results){
			List<Pair<Double, Double>> result = rs.getValues();
			for (int i = 0; i < result.size(); i++){
				Pair<Double, Double> point = result.get(i);
				if (point.getValue() != null && !point.getValue().isNaN()){
					if (point.getValue().isInfinite()){
						drawLine(gc, new Pair<Double, Double>(point.getKey(), topPos), new Pair<Double, Double>(point.getKey(), bottomPos), scaleFactor);
					} else {
						Pair<Double, Double> next = i == result.size()-1 ? null : result.get(i+1);
						if (point.getValue() < topPos+1 && point.getValue() > bottomPos-1){
							if (next != null && next.getValue() != null && !next.getValue().isNaN() && Math.abs(next.getValue()) < Integer.MAX_VALUE){
								drawLine(gc, point, next, scaleFactor);
							}
						}
					}
				}
			}
		}

		// Connect the bounds of the results if it's a quadratic equation
		if (this.quadratic){
			for (int i = 0; i < this.results.get(0).getValues().size(); i++){
				Double y1 = this.results.get(0).getValues().get(i).getValue();
				Double y2 = this.results.get(1).getValues().get(i).getValue();
				if (y1 != null && y2 != null && Math.abs(y1-y2) < 1){ // TODO: Check function's trend instead of < 1
					if (i < this.results.get(0).getValues().size()-1){
						Double ny1 = this.results.get(0).getValues().get(i+1).getValue();
						Double ny2 = this.results.get(1).getValues().get(i+1).getValue();
						if (ny1 == null && ny2 == null){
							drawLine(gc, this.results.get(0).getValues().get(i), this.results.get(1).getValues().get(i), scaleFactor);
						}
					}
					if (i > 0){
						Double py1 = this.results.get(0).getValues().get(i-1).getValue();
						Double py2 = this.results.get(1).getValues().get(i-1).getValue();
						if (py1 == null && py2 == null){
							drawLine(gc, this.results.get(0).getValues().get(i), this.results.get(1).getValues().get(i), scaleFactor);
						}
					}
				}
			}
		}
	}

	private static void drawLine(GraphicsContext gc, Pair<Double, Double> point, Pair<Double, Double> next, double scaleFactor){
		gc.strokeLine(MainApplication.WIDTH/2+point.getKey()*scaleFactor, MainApplication.HEIGHT/2-point.getValue()*scaleFactor, MainApplication.WIDTH/2+next.getKey()*scaleFactor, MainApplication.HEIGHT/2-next.getValue()*scaleFactor);
	}

	public Equation getEquation(){
		return this.equation;
	}

	public List<Result> getResults(){
		return this.results;
	}

	@Override
	public Identifier getIdentifier(){
		return Identifier.GRAPH_FUNCTION;
	}

	@Override
	public String toString(){
		return this.equation.toString();
	}
}