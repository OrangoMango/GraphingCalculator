package com.orangomango.graphcalc.math;

import java.util.*;
import java.util.function.Predicate;
import java.text.DecimalFormat;

import com.orangomango.graphcalc.Evaluator;

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
		moveAll(t -> !t.getSide(), this.leftSide, this.rightSide);
		beautify(this.leftSide);
		beautify(this.rightSide);
	}

	// TODO: Solve any kind of equation, also trigonometric ones
	public static List<Double> solve(Expression expression, Expression rightCopy, String varName, Map<String, Double> params){
		Evaluator eval = new Evaluator(rightCopy.getString(true), params); // All terms with varName must be on the left and the other ones on the right
		double a = 0;
		double b = 0;
		double c = -eval.parse();

		for (EquationPiece piece : expression.getChildren()){
			boolean hasExp = false;
			for (EquationPiece f : piece.getChildren()){
				Factor factor = (Factor)f;
				if (factor.getExponent() != null){
					hasExp = true;
				}
			}

			Map<String, Double> args = params == null ? new HashMap<>() : new HashMap<>(params);
			args.put(varName, 1.0);
			eval = new Evaluator(piece.getString(true), args);
			double sum = eval.parse();

			if (hasExp){
				a = sum;
			} else {
				b = sum;
			}
		}

		List<Double> output = new ArrayList<>();
		if (a == 0){
			double sol = -c/b;
			if (b != 0){
				output.add(sol);
			} else if (Math.abs(c) < 0.00001){ // Almost 0
				output.add(Double.POSITIVE_INFINITY);
			} else {
				output.add(Double.NaN);
			}
		} else {
			double delta = Math.sqrt(b*b-4*a*c);
			double sol1 = (-b+delta)/(2*a);
			double sol2 = (-b-delta)/(2*a);
			output.add(sol1);
			output.add(sol2);
		}

		return output;
	}

	public static void prepareForSolving(Expression expression, Expression rightCopy, String varName, Map<String, Double> params){
		moveAll(term -> {
			for (EquationPiece p : term.getChildren()){
				Factor f = (Factor)p;
				if (f.getContent() != null && f.getContent().equals(varName)){
					return true;
				}
			}
			return false;
		}, expression, rightCopy);

		expression.calculate(params);
		beautify(expression);
		beautify(rightCopy);
	}

	public Expression getLeftSide(){
		return this.leftSide;
	}

	public Expression getRightSide(){
		return this.rightSide;
	}

	private static void moveAll(Predicate<Term> condition, Expression leftSide, Expression rightSide){
		for (int i = 0; i < leftSide.getChildren().size(); i++){
			Term t = (Term)leftSide.getChildren().get(i);
			if (!condition.test(t)){
				t.changeSign();
				t.moveAll(false);
				leftSide.getChildren().remove(i);
				i--;
				rightSide.getChildren().add(t);
			}
		}
		for (int i = 0; i < rightSide.getChildren().size(); i++){
			Term t = (Term)rightSide.getChildren().get(i);
			if (condition.test(t)){
				t.changeSign();
				t.moveAll(true);
				rightSide.getChildren().remove(i);
				i--;
				leftSide.getChildren().add(t);
			}
		}

		// Left side or Right side could be empty
		if (leftSide.getChildren().size() == 0){
			Term newTerm = new Term(leftSide, true);
			Factor newFactor = new Factor(newTerm, true);
			newFactor.setContent("0");
			newTerm.getChildren().add(newFactor);
			leftSide.getChildren().add(newTerm);
		}
		if (rightSide.getChildren().size() == 0){
			Term newTerm = new Term(rightSide, false);
			Factor newFactor = new Factor(newTerm, false);
			newFactor.setContent("0");
			newTerm.getChildren().add(newFactor);
			rightSide.getChildren().add(newTerm);
		}
	}

	private static void beautify(Expression expression){
		// Remove the parenthesis and reduce
		expression.removeMinus();
		expression.rewrite();
		while (expression.canBeExpanded()){
			expression.expand();
			expression.rewrite();
		}

		// Group the common terms and factors
		// TODO: not only group by x and y
		List<Term> commonTerms = expression.getCommonTerms();
		for (Term t : commonTerms){
			Factor[] factors = new Factor[t.getChildren().size()];
			for (int i = 0; i < factors.length; i++){
				factors[i] = (Factor)t.getChildren().get(i);
			}
			boolean hasLetter = false;
			fLoop:
			for (Factor f : factors){
				String data = f.getString(true);
				for (int i = 0; i < data.length(); i++){
					char c = data.charAt(i);
					if (c == 'x' || c == 'y'){ //(c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')){
						hasLetter = true;
						break fLoop;
					}
				}
			}
			if (hasLetter && factors.length > 1){
				expression.group("*", factors);
				expression.group("/", factors);
			}
		}
		List<Factor> commonFactors = expression.getCommonFactors();
		for (Factor f : commonFactors){
			if (f.getContent() != null){
				for (int i = 0; i < f.getContent().length(); i++){
					char c = f.getContent().charAt(i);
					if (c == 'x' || c == 'y'){ //(c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')){
						expression.group("*", f);
						expression.group("/", f);
						break;
					}
				}
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

	@Override
	public String toString(){
		String result = getEquation();
		StringBuilder builder = new StringBuilder();
		DecimalFormat formatter = new DecimalFormat("#.###");
		String lastNumber = "";
		for (int i = 0; i < result.length(); i++){
			char c = result.charAt(i);
			if ((c >= '0' && c <= '9') || c == '.'){
				lastNumber += c;
			} else {
				if (!lastNumber.equals("")){
					double value = Double.parseDouble(lastNumber);
					lastNumber = "";
					builder.append(formatter.format(value));
				}
				builder.append(c);
			}
		}

		if (!lastNumber.equals("")){
			builder.append(formatter.format(Double.parseDouble(lastNumber)));
		}

		return builder.toString();
	}
}