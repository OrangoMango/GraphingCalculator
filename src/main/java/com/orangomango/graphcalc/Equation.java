package com.orangomango.graphcalc;

import java.util.*;

public class Equation{
	private String equation;
	private List<EquationTerm> terms = new ArrayList<>();

	public Equation(String e){
		this.equation = e.replace(" ", "");
		System.out.println(this.equation);

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
		this.equation = EquationTerm.formatTerm(eq.substring(1))+"=0";

		System.out.println(this.equation);
	}

	private void parseExpression(String expression, boolean left){
		List<String> parts = EquationTerm.getParts(expression, '+', '-');
		for (String part : parts){
			EquationTerm term = new EquationTerm(part, left);
			this.terms.add(term);
		}
	}
}