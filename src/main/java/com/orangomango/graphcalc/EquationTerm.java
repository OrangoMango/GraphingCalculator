package com.orangomango.graphcalc;

import java.util.*;

public class EquationTerm{
	private String term;
	private boolean left;

	public EquationTerm(String term, boolean left){
		this.term = term;
		this.left = left;
	}

	public boolean getLeft(){
		return this.left;
	}

	public void moveLeft(){
		this.left = true;
		if (this.term.startsWith("+")){
			this.term = "-"+this.term.substring(1);
		} else if (this.term.startsWith("-")){
			this.term = "+"+this.term.substring(1);
		}
	}

	public String getTerm(){
		return this.term;
	}

	public List<EquationTerm> reduce(){
		List<EquationTerm> output = new ArrayList<>();
		output.add(this);
		int foundIndex = -1;
		do {
			boolean termFound = false;
			for (int i = 0; i < output.size(); i++){
				if (output.get(i).term.contains("(")){
					foundIndex = i;
					termFound = true;
					break;
				}
			}
			if (termFound){
				EquationTerm eq = output.remove(foundIndex);
				List<String> parts = EquationTerm.getParts(eq.term, '*', '/');
				if (parts.size() == 1){
					String newTermString = compress(this.term.substring(2, this.term.length()-1));
					if (!newTermString.startsWith("-")) newTermString = "+"+newTermString;
					output.add(new EquationTerm(newTermString, this.left));
				} else {
					int index = -1;
					for (int j = 0; j < parts.size(); j++){
						String p = parts.get(j);
						if (p.contains("(")){
							index = j;
							break;
						}
					}
					if (index == 0){
						List<String> toMultiply = getParts(parts.get(index).substring(3, parts.get(index).length()-1), '+', '-');
						String factor = parts.get(index+1);
						for (String p : toMultiply){
							String newTermString = compress(p+factor);
							if (!newTermString.startsWith("-")) newTermString = "+"+newTermString;
							EquationTerm newTerm = new EquationTerm(newTermString, this.left);
							output.add(newTerm);
						}
					} else {
						List<String> toMultiply = getParts(parts.get(index).substring(2, parts.get(index).length()-1), '+', '-');
						StringBuilder factor = new StringBuilder();
						for (int j = 0; j < index; j++){
							factor.append(parts.get(j));
						}
						for (String p : toMultiply){
							String newTermString = compress(factor.toString().substring(1)+parts.get(index).charAt(0)+p);
							if (!newTermString.startsWith("-")) newTermString = "+"+newTermString;
							EquationTerm newTerm = new EquationTerm(newTermString, this.left);
							output.add(newTerm);
						}
					}
				}
			} else {
				break; // All terms are reduced
			}
		} while (true);

		return output;
	}

	public static String compress(String term){
		List<String> parts = getParts(term, '*', '/');
		String evalString = "";
		String lettersString = "";

		for (String part : parts){
			boolean number = true;
			for (int i = 0; i < part.length(); i++){
				char c = part.charAt(i);
				if ((c >= 'a' && c <= 'z')){
					number = false;
					break;
				}
			}
			if (part.charAt(1) == '+') part = part.charAt(0)+part.substring(2);
			if (part.charAt(1) == '-') part = part.charAt(0)+"(-"+part.substring(2)+")";
			if (number){
				evalString += part;
			} else {
				lettersString += part;
			}
		}
		
		Double finalNumber = null;
		if (!evalString.equals("")){
			Evaluator eval = new Evaluator(evalString.substring(1), null);
			finalNumber = eval.parse();
		}

		if (finalNumber == null){
			return lettersString.substring(1);
		} else {
			return finalNumber+lettersString;
		}
	}

	public static List<String> getParts(String expression, char c1, char c2){
		List<String> output = new ArrayList<>();
		String currentPart = "";
		if (expression.charAt(0) == c1){
			currentPart = String.valueOf(c1);
			expression = expression.substring(1);
		} else if (expression.charAt(0) == c2){
			currentPart = String.valueOf(c2);
			expression = expression.substring(1);
		}
		String before = String.valueOf(c1);
		int locked = 0;
		for (int i = 0; i < expression.length(); i++){
			char c = expression.charAt(i);
			if ((locked == 0 && (c == c1 || c == c2)) || i == expression.length()-1){
				if (i == expression.length()-1){
					currentPart += c;
					if (locked != 0 && c != ')'){
						throw new RuntimeException("Missing ')'");
					}
				}
				if (i > 0 || i == expression.length()-1) output.add(before+currentPart);
				before = String.valueOf(c);
				currentPart = "";
			} else {
				currentPart += c;
				if (c == '('){
					locked++;
				} else if (c == ')'){
					locked--;
				}
			}
		}

		return output;
	}

	@Override
	public String toString(){
		return this.term+(this.left ? " [L]" : " [R]");
	}
}