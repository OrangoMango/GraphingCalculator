package com.orangomango.graphcalc;

import java.util.*;
import java.util.function.Function;

public class Evaluator{
	private String expression;
	private int pos;
	private Map<String, Double> variables;

	public Evaluator(String input, Map<String, Double> vars){
		this.expression = input;
		this.variables = vars;
	}

	public static Function<Double, Double> buildFunction(String expression, String argument, Map<String, Double> v){
		Map<String, Double> vars = v == null ? new HashMap<>() : new HashMap<>(v);
		Evaluator eval = new Evaluator(expression, vars);
		return x -> {
			vars.put(argument, x);
			return eval.parse();
		};
	}

	/*private static String format(String text){
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < text.length(); i++){
			char c = text.charAt(i);
			if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) && i > 0){
				char prev = text.charAt(i-1);
				if (prev != '+' && prev != '-' && prev != '*' && prev != '/' && prev != '^'){
					output.append("*").append(c);
				} else {
					output.append(c);
				}
			} else {
				output.append(c);
			}
		}

		return output.toString();
	}*/

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

	public double parse(){
		this.pos = 0;
		double x = parseExpression();
		if (this.pos != this.expression.length()-1){
			throw new RuntimeException("Could not parse: "+this.expression.substring(this.pos));
		}
		return x;
	}

	private double parseExpression(){
		double x = parseTerm();
		while (true){
			if (skip('+')){
				x += parseTerm();
			} else if (skip('-')){
				x -= parseTerm();
			} else {
				break;
			}
		}

		return x;
	}

	private double parseTerm(){
		double x = parseFactor();
		while (true){
			if (skip('*')){
				x *= parseFactor();
			} else if (skip('/')){
				x /= parseFactor();
			} else {
				break;
			}
		}

		return x;
	}

	private double parseFactor(){
		StringBuilder builder = new StringBuilder();
		String funcName = "";
		char c = nextChar();
		if (c == '+') return parseFactor();
		else if (c == '-') return -parseFactor();

		while (true){
			if ((c >= '0' && c <= '9') || c == '.'){
				builder.append(c);
			} else if (c == '('){
				builder.append(Double.toString(parseExpression()));
				if (!skip(')')){
					throw new RuntimeException("Expected ')'");
				}
			} else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')){
				funcName += c;
			} else {
				this.pos--;
				break;
			}
			c = nextChar();
		}

		String value = builder.toString();
		Double output = Double.NaN;
		if (value.equals("")){
			if (funcName.equals("PI")){
				output = Math.PI;
			} else if (funcName.equals("E")){
				output = Math.E;
			} else if (this.variables != null && this.variables.keySet().contains(funcName)){
				output = this.variables.get(funcName);
			}
		} else {
			final double x = Double.parseDouble(value);
			if (funcName.equals("")){
				output = x;
			} else {
				if (funcName.equals("sqrt")){
					output = Math.sqrt(x);
				} else if (funcName.equals("abs")){
					output = Math.abs(x);
				} else if (funcName.equals("sin")){
					output = Math.sin(x);
				} else if (funcName.equals("cos")){
					output = Math.cos(x);
				} else if (funcName.equals("tan")){
					output = Math.tan(x);
				}
			}
		}

		if (!output.isNaN()){
			if (skip('^')){
				double exp = parseFactor();
				return Math.pow(output, exp);
			} else {
				return output;
			}
		} else {
			throw new RuntimeException("Unknown function/variable: "+funcName);
		}
	}
}