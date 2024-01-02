package com.orangomango.graphcalc;

import java.util.*;

public class EquationTerm{
	private String term;
	private boolean left;

	public EquationTerm(String term, boolean left){
		this.term = term;
		this.left = left;
	}

	public void move(boolean v){
		if (this.left != v){
			this.left = v;
			if (this.term.startsWith("+")){
				this.term = "-"+this.term.substring(1);
			} else if (this.term.startsWith("-")){
				this.term = "+"+this.term.substring(1);
			}
		}
	}

	public String getTerm(){
		return this.term;
	}

	public boolean getLeft(){
		return this.left;
	}

	public List<EquationTerm> reduce(){
		//System.out.println("Reducing "+this);
		List<EquationTerm> output = new ArrayList<>();
		output.add(this);
		int foundIndex = -1;
		do {
			//System.out.println("output: "+output);
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
				//System.out.println("parts: "+parts);
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
						//System.out.println("\n>"+parts.get(index)+"\n");
						List<String> toMultiply = getParts(parts.get(index).substring(parts.get(index).charAt(2) == '+' || parts.get(index).charAt(2) == '-' ? 2 : 3, parts.get(index).length()-1), '+', '-');
						//System.out.println("toMultiply[0]: "+toMultiply);
						String factor = parts.get(index+1);
						for (String p : toMultiply){
							String newTermString = compress(p+factor);
							if (!newTermString.startsWith("-")) newTermString = "+"+newTermString;
							EquationTerm newTerm = new EquationTerm(newTermString, this.left);
							output.add(newTerm);
						}
					} else {
						//System.out.println("\n>>"+parts.get(index)+"\n");
						List<String> toMultiply = getParts(parts.get(index).substring(2, parts.get(index).length()-1), '+', '-');
						//System.out.println("toMultiply: "+toMultiply);
						StringBuilder factor = new StringBuilder();
						for (int j = 0; j < index; j++){
							factor.append(parts.get(j));
						}
						if (index < parts.size()-1){
							String tempString = "";
							for (String p : toMultiply){
								String newTermString = compress(factor.toString().substring(1)+parts.get(index).charAt(0)+p);
								if (!newTermString.startsWith("-")) newTermString = "+"+newTermString;
								tempString += newTermString;
							}
							String missing = "";
							for (int j = index+1; j < parts.size(); j++){
								missing += parts.get(j);
							}
							output.add(new EquationTerm("("+tempString+")"+missing, this.left));
						} else {
							for (String p : toMultiply){
								String newTermString = compress(factor.toString().substring(1)+parts.get(index).charAt(0)+p);
								if (!newTermString.startsWith("-")) newTermString = "+"+newTermString;
								EquationTerm newTerm = new EquationTerm(newTermString, this.left);
								output.add(newTerm);
							}
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
			if (evalString.startsWith("*")){
				evalString = evalString.substring(1);
			} else if (evalString.startsWith("/")){
				evalString = "1"+evalString;
			}
			Evaluator eval = new Evaluator(evalString, null);
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
		String before = String.valueOf(c1);
		if (expression.charAt(0) == c1){
			before = String.valueOf(c1);
			expression = expression.substring(1);
		} else if (expression.charAt(0) == c2){
			before = String.valueOf(c2);
			expression = expression.substring(1);
		}
		int locked = 0;
		for (int i = 0; i < expression.length(); i++){
			char c = expression.charAt(i);
			if ((locked == 0 && (c == c1 || c == c2) && (i == 0 || expression.charAt(i-1) != 'E')) || i == expression.length()-1){
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