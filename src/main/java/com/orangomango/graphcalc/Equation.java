package com.orangomango.graphcalc;

import java.util.*;

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
		for (EquationTerm term : this.terms){
			if (!term.getLeft()){
				term.moveLeft();
			}
		}
		String eq = "";
		for (EquationTerm term : this.terms){
			eq += term.getTerm();
		}

		// Final step
		this.equation = format(beautify(eq))+"=0";
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
					result.add(Double.toString(pieces.stream().mapToDouble(Double::parseDouble).sum()));
				} else {
					double sum = 0;
					for (int i = 0; i < pieces.size(); i++){
						String piece = pieces.get(i);
						if (piece.contains("*")){
							sum += Double.parseDouble(piece.split("\\*")[0]);
						} else {
							// TODO 1/x and 1/y, and a^x
							// ...
							sum += 1;
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