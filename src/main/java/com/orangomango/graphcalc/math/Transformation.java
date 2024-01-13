package com.orangomango.graphcalc.math;

import java.util.*;

import com.orangomango.graphcalc.Evaluator;

public class Transformation{
	private String xEq, yEq;

	public Transformation(String e1, String e2){
		System.out.println("t: "+e1+", "+e2);
		Equation eq1 = new Equation(e1.replace("x'", "0"));
		Equation eq2 = new Equation(e2.replace("y'", "0"));

		List<Double> co1 = getCoefficients(eq1);
		List<Double> co2 = getCoefficients(eq2);
		System.out.println(co1+" "+co2);
		double[][] mat = new double[][]{{co1.get(0), co1.get(1)}, {co2.get(0), co2.get(1)}}; // must be read as mat[y][x]
		double det = mat[0][0]*mat[1][1]-mat[0][1]*mat[1][0];
		if (det != 0){
			double[][] result = new double[][]{{mat[1][1], -mat[1][0]}, {-mat[0][1], mat[0][0]}}; // must be read as mat[x][y]
			String f1 = getFactor(co1.get(2), "x");
			String f2 = getFactor(co2.get(2), "y");
			this.xEq = getStringEq(result, det, 0, f1, f2);
			this.yEq = getStringEq(result, det, 1, f1, f2);
		} else {
			throw new IllegalStateException("det is 0");
		}
	}

	private String getFactor(double n, String l){
		if (n == 0){
			return l;
		} else {
			if (n >= 0){
				return "("+l+"-"+Double.toString(Math.abs(n))+")";
			} else {

				return "("+l+"+"+Double.toString(Math.abs(n))+")";
			}
		}
	}

	public String getDefX(){
		return this.xEq;
	}

	public String getDefY(){
		return this.yEq;
	}

	private String getStringEq(double[][] mat, double det, int pos, String factor1, String factor2){
		double p1 = 1/det*mat[0][pos];
		double p2 = 1/det*mat[1][pos];
		StringBuilder builder = new StringBuilder();
		if (p1 != 0){
			if (Math.abs(p1) == 1){
				builder.append(p1 > 0 ? "" : "-").append(factor1);
			} else {
				builder.append(Double.toString(p1)).append("*"+factor1);
			}
		}
		if (p2 != 0){
			if (p2 < 0 || p1 != 0) builder.append(p2 >= 0 ? "+" : "-");
			if (Math.abs(p2) == 1){
				builder.append(factor2);
			} else {
				builder.append(Double.toString(Math.abs(p2))).append("*"+factor2);
			}
		}

		String eq = builder.toString();
		if (eq.startsWith("(") && eq.endsWith(")")) eq = eq.substring(1, eq.length()-1);
		return eq;
	}

	private static List<Double> getCoefficients(Equation equation){
		List<EquationPiece> terms = equation.getLeftSide().getChildren();
		Map<String, Double> output = new HashMap<>();
		double c = 0;
		for (EquationPiece piece : terms){
			Map<String, Double> params = new HashMap<>();
			params.put("x", 1.0);
			params.put("y", 1.0);

			Evaluator eval = new Evaluator(piece.getString(true), params);
			double value = eval.parse();
			if (piece.getString(true).contains("x")){
				output.put("x", value);
			} else if (piece.getString(true).contains("y")){
				output.put("y", value);
			} else {
				c = value;
			}
		}

		List<Double> result = new ArrayList<>();
		result.add(-output.getOrDefault("x", 0.0));
		result.add(-output.getOrDefault("y", 0.0));
		result.add(-c);

		return result;
	}
}