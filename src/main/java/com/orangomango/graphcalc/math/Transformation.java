package com.orangomango.graphcalc.math;

import java.util.*;

import com.orangomango.graphcalc.Evaluator;

public class Transformation{
	public Transformation(String e1, String e2){
		Equation eq1 = new Equation(e1);
		Equation eq2 = new Equation(e2);

		List<Double> co1 = getCoefficients(eq1);
		System.out.println(co1);
		List<Double> co2 = getCoefficients(eq2);
		System.out.println(co2);

		System.out.println(eq1);
		System.out.println(eq2);
		System.out.println("getX(): "+getX(co1, co2));
		System.out.println("getY(): "+getY(co1, co2));

		eq1 = new Equation(getX(co1, co2)+"=0");
		eq2 = new Equation(getY(co1, co2)+"=0");

		System.out.println(eq1);
		System.out.println(eq2);
	}

	private String getX(List<Double> co1, List<Double> co2){
		double a1 = co1.get(0);
		double b1 = co1.get(1);
		double c1 = co1.get(2);

		return String.format("(x%s*(%s)%s)/%s", writeNumber(-b1), getY(co1, co2), writeNumber(-c1), a1 < 0 ? "("+a1+")" : a1);
	}

	private String getY(List<Double> co1, List<Double> co2){
		double a1 = co1.get(0);
		double b1 = co1.get(1);
		double c1 = co1.get(2);
		double a2 = co2.get(0);
		double b2 = co2.get(1);
		double c2 = co2.get(2);

		return String.format("(%s*x%s*y%s%s)/(%s%s)", -a2, writeNumber(a1), writeNumber(a2*c1), writeNumber(-a1*c2), a1*b2, writeNumber(-a2*b1));
	}

	private String writeNumber(double num){
		if (num >= 0){
			return "+"+Math.abs(num);
		} else {
			return Double.toString(num);
		}
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