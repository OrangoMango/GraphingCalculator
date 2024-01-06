package com.orangomango.graphcalc.math;

import java.util.*;
import java.util.function.Predicate;

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
		moveAll(t -> !t.getSide());
		beautify(this.leftSide);
		beautify(this.rightSide);
	}

	public List<Double> solve(String varName, Map<String, Double> params){
		//System.out.println("> "+this.leftSide.getString(true)+"="+this.rightSide.getString(true));
		moveAll(term -> {
			for (EquationPiece p : term.getChildren()){
				Factor f = (Factor)p;
				//System.out.println("Checking: "+f.getString(true));
				if (f.getContent() != null && f.getContent().equals(varName)){
					//System.out.println("\tReturned true");
					return true;
				}
			}
			return false;
		});
		//System.out.println("> "+this.leftSide.getString(true)+"="+this.rightSide.getString(true));

		Expression expression = (Expression)this.leftSide.copy(null);
		expression.calculate(params);
		//System.out.println("> "+expression.getString(true)+"="+this.rightSide.getString(true));
		beautify(expression);

		//System.out.println("> "+expression.getString(true)+"="+this.rightSide.getString(true));

		Evaluator eval = new Evaluator(this.rightSide.getString(true), params);
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
			output.add(sol);
		} else {
			double delta = Math.sqrt(b*b-4*a*c);
			double sol1 = (-b+delta)/(2*a);
			double sol2 = (-b-delta)/(2*a);
			output.add(sol1);
			output.add(sol2);
		}

		return output;
	}

	public Expression getLeftSide(){
		return this.leftSide;
	}

	public Expression getRightSide(){
		return this.rightSide;
	}

	public void moveAll(Predicate<Term> condition){
		for (int i = 0; i < this.leftSide.getChildren().size(); i++){
			Term t = (Term)this.leftSide.getChildren().get(i);
			if (!condition.test(t)){
				t.changeSign();
				t.moveAll(false);
				this.leftSide.getChildren().remove(i);
				i--;
				this.rightSide.getChildren().add(t);
			}
		}
		for (int i = 0; i < this.rightSide.getChildren().size(); i++){
			Term t = (Term)this.rightSide.getChildren().get(i);
			if (condition.test(t)){
				t.changeSign();
				t.moveAll(true);
				this.rightSide.getChildren().remove(i);
				i--;
				this.leftSide.getChildren().add(t);
			}
		}

		//System.out.println("Right after moving: "+this.leftSide.getString(true)+"="+this.rightSide.getString(true));
		
		// Left side or Right side could be empty
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

	private static void beautify(Expression expression){
		// Remove the parenthesis and reduce
		expression.removeMinus();
		expression.rewrite();
		while (expression.canBeExpanded()){
			expression.expand();
			expression.rewrite();
			//System.out.println("AFTER rewrite:\n"+expression.getString(true));
		}

		//System.out.println("\n"+expression.getString(true)+"\n");

		// Group the common terms and factors
		//System.out.println("\n\n> By terms");
		List<Term> commonTerms = expression.getCommonTerms();
		//System.out.println(commonTerms.stream().map(ep -> ep.getString(true)).toList());
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
		//System.out.println("AFTER grouping1: "+expression.getString(true));
		//System.out.println("> By factors");
		List<Factor> commonFactors = expression.getCommonFactors();
		//System.out.println(commonFactors.stream().map(ep -> ep.getString(true)).toList());
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

		//System.out.println("AFTER grouping2: "+expression.getString(true));

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