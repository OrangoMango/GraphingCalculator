package com.orangomango.graphcalc.math;

import java.util.*;
import java.util.function.Predicate;

public class Equation{
	private Expression leftSide, rightSide;

	public Equation(String e){
		e = formatExponential(e.replace(" ", ""));

		Parser parser;
		// Parse the left side of the equation
		parser = new Parser(e.split("=")[0], true);
		this.leftSide = parser.parse();
		// Parse the right side of the equation
		parser = new Parser(e.split("=")[1], false);
		this.rightSide = parser.parse();

		// Move everything to one side (left)
		moveAll(t -> !t.getSide());
	}

	/*public List<Double> solve(String varName, Map<String, Double> params){
		moveAll(term -> {
			for (EquationPiece p : term.getChildren()){
				Factor f = (Factor)p;
				if (f.getContent() != null && f.getContent().equals(varName)){
					return true;
				}
			}
			return false;
		});

		this.leftSide.calculate(params);
		this.rightSide.calculate(params);

		return null;
	}*/

	public Expression getLeftSide(){
		return this.leftSide;
	}

	public Expression getRightSide(){
		return this.rightSide;
	}

	private void fix(){
		if (this.leftSide.getChildren().size() == 0){
			Term newTerm = new Term(this.leftSide, true);
			Factor newFactor = new Factor(newTerm, true);
			newFactor.setContent("0");
			newTerm.getChildren().add(newFactor);
			this.leftSide.getChildren().add(newTerm);
		}
		if (this.rightSide.getChildren().size() == 0){
			Term newTerm = new Term(this.rightSide, false);
			Factor newFactor = new Factor(newTerm, false);
			newFactor.setContent("0");
			newTerm.getChildren().add(newFactor);
			this.rightSide.getChildren().add(newTerm);
		}
	}

	public void moveAll(Predicate<Term> condition){
		List<EquationPiece> toRemove = new ArrayList<>();
		for (EquationPiece p : this.leftSide.getChildren()){
			Term t = (Term)p;
			if (!condition.test(t)){
				t.changeSign();
				t.moveAll(false);
				toRemove.add(t);
			}
		}
		for (EquationPiece p : toRemove){
			this.leftSide.getChildren().remove(p);
			this.rightSide.getChildren().add(p);
		}
		toRemove.clear();
		for (EquationPiece p : this.rightSide.getChildren()){
			Term t = (Term)p;
			if (condition.test(t)){
				t.changeSign();
				t.moveAll(true);
				toRemove.add(t);
			}
		}
		for (EquationPiece p : toRemove){
			this.rightSide.getChildren().remove(p);
			this.leftSide.getChildren().add(p);
		}

		beautify(this.leftSide);
		beautify(this.rightSide);
		fix();
	}

	private static void beautify(Expression expression){
		// Remove the parenthesis and reduce
		expression.removeMinus();
		while (expression.canBeReduced()){
			expression.reduce();
		}

		// Rewrite the expression
		expression.rewrite();

		// Group the common factors
		List<Factor> common = expression.getCommonFactors();
		for (Factor f : common){
			try {
				Integer.parseInt(f.getContent()); // TODO: Use regex
			} catch (NumberFormatException ex){
				expression.group(f, "*");
				expression.group(f, "/");
			}
		}

		// Rewrite the expression again
		expression.rewrite();
	}

	public static String formatExponential(String input){
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < input.length(); i++){
			char c = input.charAt(i);
			if (c == 'E' && i > 0){
				char prev = input.charAt(i-1);
				if (prev >= '0' && prev <= '9'){
					c = '^';
				}
			}
			builder.append(c);
		}

		return builder.toString();
	}

	public String getEquation(){
		return this.leftSide.getString(true)+"="+this.rightSide.getString(true);
	}
}