package com.orangomango.graphcalc.math;

import java.util.*;

public class Equation{
	private class Parser{
		private int pos;
		private String expression;
		private boolean left;

		public Parser(String expression, boolean left){
			this.expression = expression;
			this.left = left;
		}

		private char nextChar(){
			return this.pos < this.expression.length() ? this.expression.charAt(this.pos++) : '\0';
		}

		private boolean skip(char c){
			char s;
			do {
				s = nextChar();
				if (s == c){
					return true;
				}
			} while (s == ' ');
			this.pos--;

			return false;
		}

		public Expression parse(){
			this.pos = 0;
			Expression exp = new Expression(null, this.left);
			parseExpression(exp);
			if (this.pos != this.expression.length()-1){
				throw new RuntimeException("Could not parse: "+this.expression.substring(this.pos));
			}
			return exp;
		}

		private void parseExpression(Expression expression){
			Term term = new Term(expression, this.left);
			parseTerm(term);
			expression.getChildren().add(term);
			while (true){
				if (skip('+')){
					term = new Term(expression, this.left);
					parseTerm(term);
					term.prefix = "+";
					expression.getChildren().add(term);
				} else if (skip('-')){
					term = new Term(expression, this.left);
					parseTerm(term);
					term.prefix = "-";
					expression.getChildren().add(term);
				} else {
					break;
				}
			}
		}

		private void parseTerm(Term term){
			Factor factor = new Factor(term, this.left);
			parseFactor(factor);
			term.getChildren().add(factor);
			while (true){
				if (skip('*')){
					factor = new Factor(term, this.left);
					parseFactor(factor);
					factor.prefix = "*";
					term.getChildren().add(factor);
				} else if (skip('/')){
					factor = new Factor(term, this.left);
					parseFactor(factor);
					factor.prefix = "/";
					term.getChildren().add(factor);
				} else {
					break;
				}
			}
		}

		private void parseFactor(Factor factor){
			StringBuilder builder = new StringBuilder();
			String funcName = "";
			char c = nextChar();
			if (c == '+'){
				factor.prefix = "+";
				parseFactor(factor);
				return;
			} else if (c == '-'){
				factor.prefix = "-";
				parseFactor(factor);
				return;
			}

			while (true){
				if (c == '('){
					Expression exp = new Expression(factor, this.left);
					parseExpression(exp);
					if (!skip(')')){
						throw new RuntimeException("Expected ')'");
					}
					if (funcName.equals("")){
						factor.getChildren().add(exp);
					} else {
						factor.setArgument(exp);
					}
				} else if ((c >= '0' && c <= '9') || c == '.'){
					builder.append(c);
				} else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')){
					funcName += c;
				} else {
					this.pos--;
					break;
				}
				c = nextChar();
			}

			String output = funcName.equals("") ? builder.toString() : funcName;

			if (skip('^')){
				Factor exp = new Factor(factor, this.left);
				parseFactor(exp);
				factor.setExponent(exp);
			}

			factor.setContent(output);
		}
	}

	private Expression equation;

	public Equation(String e){
		e = e.replace(" ", "");

		Parser parser = new Parser(e.split("=")[0], true);
		Expression leftPart = parser.parse();
		parser = new Parser(e.split("=")[1], false);
		Expression rightPart = parser.parse();

		// Move everything to one side
		rightPart.moveAll(true);
		for (EquationPiece p : rightPart.getChildren()){
			Term t = (Term)p;
			t.changeSign();
		}
		leftPart.add(rightPart);
		this.equation = leftPart;

		this.equation.reduce();

		System.out.println(this.equation);
	}

	public String getEquation(){
		return null;
	}
}