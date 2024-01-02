package com.orangomango.graphcalc;

import java.util.*;
import java.util.function.Predicate;

public class Equation{
	private String equation;
	private List<EquationTerm> terms = new ArrayList<>();

	public Equation(String e){
		this.equation = e.replace(" ", "").replace("-(", "-1*(");

		String leftPart = this.equation.split("=")[0];
		String rightPart = this.equation.split("=")[1];
		parseExpression(leftPart, true);
		parseExpression(rightPart, false);

		List<EquationTerm> temp = new ArrayList<>();
		for (int i = 0; i < this.terms.size(); i++){
			EquationTerm term = this.terms.get(i);
			List<EquationTerm> reduced = term.reduce();
			temp.addAll(reduced);
			if (reduced.size() == 0){
				temp.add(term);
			}
		}
		this.terms = temp;

		// Move all the terms on one side
		moveAllTerms(term -> !term.getLeft(), true);

		// Final step
		this.equation = format(beautify(this.equation.split("=")[0]))+"=0";
	}

	public void moveAllTerms(Predicate<EquationTerm> condition, boolean left){
		for (EquationTerm term : this.terms){
			if (condition.test(term)){
				term.move(left);
			}
		}

		StringBuilder result = new StringBuilder();
		int count = 0;
		for (EquationTerm term : this.terms){
			if (term.getLeft()){
				result.append(term.getTerm());
				count++;
			}
		}
		if (count == 0) result.append("0");
		count = 0;
		result.append("=");
		for (EquationTerm term : this.terms){
			if (!term.getLeft()){
				result.append(term.getTerm());
				count++;
			}
		}
		if (count == 0) result.append("0");
		this.equation = result.toString();
	}

	public List<Double> solve(String varName, Map<String, Double> params){
		String eq = this.equation.split("=")[0];
		if (params != null){
			for (Map.Entry<String, Double> entry : params.entrySet()){
				eq = eq.replace(entry.getKey(), "("+entry.getValue()+")");
			}
		}
		eq = format(eq);

		double[] cf = getCoefficients(eq, varName);
		List<Double> output = new ArrayList<>();

		if (cf[0] == 0){
			output.add(-cf[2]/cf[1]);
		} else {
			double delta = cf[1]*cf[1]-4*cf[0]*cf[2];
			double s1 = (-cf[1]+Math.sqrt(delta))/(2*cf[0]);
			double s2 = (-cf[1]-Math.sqrt(delta))/(2*cf[0]);
			output.add(s1);
			output.add(s2);
		}

		return output;
	}

	public static double[] getCoefficients(String eq, String varName){
		List<String> parts = EquationTerm.getParts(eq.split("=")[0], '+', '-');

		double a = 0;
		double b = 0;
		double c = 0;
		for (int i = 0; i < parts.size(); i++){
			String part = parts.get(i);
			if (!part.contains("*"+varName)){
				c = Double.parseDouble(part);
			} else if (part.contains("*"+varName+"^2")){
				a = Double.parseDouble(part.split("\\*")[0]);
			} else if (part.contains("*"+varName)){
				b = Double.parseDouble(part.split("\\*")[0]);
			}
		}

		return new double[]{a, b, c};
	}

	private static String beautify(String equation){
		if (equation.startsWith("+")){
			equation = equation.substring(1);
		}

		equation = equation.replace("x*x", "x^2");
		equation = equation.replace("y*y", "y^2");

		return equation;
	}

	private static String format(String term){
		List<String> parts = EquationTerm.getParts(term, '+', '-');
		if (parts.size() > 1){
			Map<String, List<String>> groups = new HashMap<>();
			for (String part : parts){
				String v = "D"; // Numbers are located in group 'D' (hardcoded)
				for (int i = 0; i < part.length(); i++){
					char c = part.charAt(i);
					if (c >= 'a' && c <= 'z'){
						v = String.valueOf(c);
						if (i < part.length()-2 && part.charAt(i+1) == '^'){
							v = c+"^"+part.charAt(i+2);
						}
						break;
					}
				}

				List<String> factors = groups.getOrDefault(v, new ArrayList<String>());
				groups.put(v, factors);
				factors.add(part);
			}

			List<String> result = new ArrayList<>();
			for (Map.Entry<String, List<String>> group : groups.entrySet()){
				List<String> pieces = new ArrayList<>();
				for (String part : group.getValue()){
					pieces.add(EquationTerm.compress(part));
				}

				String varName = null;
				for (int i = 0; i < pieces.get(0).length(); i++){
					char c = pieces.get(0).charAt(i);
					if ((c >= 'a' && c <= 'z')){
						varName = String.valueOf(c);
						if (i < pieces.get(0).length()-2 && pieces.get(0).charAt(i+1) == '^'){
							varName = c+"^"+pieces.get(0).charAt(i+2);
						}
						break;
					}
				}
				if (group.getKey().equals("D")){
					double sum = pieces.stream().mapToDouble(Double::parseDouble).sum();
					result.add(Double.toString(sum));
				} else {
					double sum = 0;
					for (int i = 0; i < pieces.size(); i++){
						String piece = pieces.get(i);
						if (piece.contains("*")){
							sum += Double.parseDouble(piece.split("\\*")[0]);
						} else {
							// TODO 1/x and 1/y, and a^x
							// ...
							String p = piece.replace(varName, "*"+varName).replace("-*"+varName, "-1*"+varName);
							if (p.startsWith("(") && p.endsWith(")")){
								p = p.substring(1, p.length()-1);
							}
							sum += p.split("\\*")[0].equals("") ? 1 : Double.parseDouble(p.split("\\*")[0]);
						}
					}
					result.add(sum+"*"+varName);
				}
			}

			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < result.size(); i++){
				if (i != 0 && !result.get(i).startsWith("-")) builder.append("+");
				builder.append(result.get(i));
			}
			return builder.toString();
		} else {
			return parts.get(0).substring(1);
		}
	}

	public String getEquation(){
		return this.equation;
	}

	private void parseExpression(String expression, boolean left){
		List<String> parts = EquationTerm.getParts(expression, '+', '-');
		for (String part : parts){
			EquationTerm term = new EquationTerm(part, left);
			this.terms.add(term);
		}
	}
}