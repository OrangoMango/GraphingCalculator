package com.orangomango.graphcalc.math;

import java.util.*;
import java.util.function.Consumer;

public class Expression extends EquationPiece{
	public Expression(EquationPiece parent, boolean left){
		super(parent, left);
	}

	public List<Term> getCommonTerms(){
		List<Term> output = new ArrayList<>();
		for (EquationPiece piece : this.pieces){
			if (!output.contains(piece)){
				output.add((Term)piece);
			}
		}

		return output;
	}

	public List<Factor> getCommonFactors(){
		List<Factor> output = new ArrayList<>();
		for (EquationPiece piece : this.pieces){
			for (EquationPiece f : piece.getChildren()){
				Factor factor = (Factor)f;
				if (!output.contains(factor)){
					output.add(factor);
				}
			}
		}

		return output;
	}

	public boolean canBeExpanded(){
		for (EquationPiece piece : this.pieces){
			if (piece.getChildren().size() >= 2){
				boolean found1 = false;
				boolean found2 = false;
				for (EquationPiece f : piece.getChildren()){
					Factor factor = (Factor)f;
					if (factor.getExpression() != null && !found1){
						found1 = true;
					} else {
						found2 = true;
					}
					if (found1 && found2){
						return true;
					}
				}
			}
		}
		return false;
	}

	public void calculate(Map<String, Double> params){
		/*System.out.format("Terms of %s:\n", this.getString(true));
		for (EquationPiece piece : this.pieces){
			System.out.println(piece+"\n");
		}*/

		for (EquationPiece piece : this.pieces){
			for (EquationPiece f : piece.getChildren()){
				Factor factor = (Factor)f;
				Expression exp = factor.getExpression();
				try {
					Evaluator eval = new Evaluator(factor.getString(true).substring(1), params);
					double value = eval.parse();
					if (exp != null) factor.getChildren().clear();
					if (value < 0){
						((Term)factor.getParent()).changeSign();
					}
					if (factor.getArgument() == null) factor.setContent(Double.toString(Math.abs(value)));
				} catch (RuntimeException ex){}
			}
		}

		for (EquationPiece piece : this.pieces){
			try {
				Evaluator eval = new Evaluator(piece.getString(true), params);
				double value = eval.parse();
				if (value < 0){
					piece.prefix = "-";
				}
				piece.getChildren().clear();
				Factor newFactor = new Factor(piece, this.left);
				newFactor.setContent(Double.toString(Math.abs(value)));
				piece.getChildren().add(newFactor);
			} catch (RuntimeException ex){}
		}

		applyToChildren(expr -> expr.calculate(params));
	}

	public void rewrite(){
		// Rewrite with exponents, x*x becomes x^2
		for (EquationPiece piece : this.pieces){
			Map<String, Integer> count = new HashMap<>();
			for (EquationPiece p : piece.getChildren()){
				String disp = p.getString(true).substring(1);
				for (int i = 0; i < disp.length(); i++){
					char c = disp.charAt(i);
					if (c >= 'a' && c <= 'z'){
						count.put(disp, count.getOrDefault(disp, 0)+1);
						break;
					}
				}
			}
			for (Map.Entry<String, Integer> entry : count.entrySet()){
				if (entry.getValue() > 1){
					boolean first = true;
					for (int i = 0; i < piece.getChildren().size(); i++){
						EquationPiece p = piece.getChildren().get(i);
						if (p.getString(true).substring(1).equals(entry.getKey())){
							if (first){
								Factor factor = (Factor)p;
								Factor exp = new Factor(factor, this.left);
								exp.setContent(Integer.toString(entry.getValue()));
								factor.setExponent(exp);
								first = false;
							} else {
								piece.getChildren().remove(i);
								i--;
							}
						}
					}
				}
			}
		}

		// Handle expressions that have an exponent
		for (EquationPiece piece : this.pieces){
			final int size = piece.getChildren().size();
			for (int i = 0; i < size; i++){
				Factor factor = (Factor)piece.getChildren().get(i);
				if (factor.getExpression() != null && factor.getExponent() != null){
					if (factor.getExponent().prefix.equals("+")){
						try {
							int times = Integer.parseInt(factor.getExponent().getContent()); // TODO: ^0 = 1 -> remove
							factor.setExponent(null);
							for (int j = 0; j < times-1; j++){
								Factor copy = (Factor)factor.copy(piece);
								copy.prefix = "*";
								piece.getChildren().add(copy);
							}
						} catch (NumberFormatException ex){}
					}
				}
			}
		}

		for (int i = 0; i < this.pieces.size(); i++){
			EquationPiece piece = this.pieces.get(i);
			for (int j = 0; j < piece.getChildren().size(); j++){
				Factor f = (Factor)piece.getChildren().get(j);
				if (f.getContent() != null){
					try {
						double x = Double.parseDouble(f.getContent());
						if (x == 0 && this.pieces.size() > 1){
							// Remove factors that have content '0'
							this.pieces.remove(i);
							i--;
						} else if (x == 1 && (f.prefix.equals("*") || f.prefix.equals("/"))){
							// Remove factors that have a term-index > 0 and content = '1'
							piece.getChildren().remove(j);
							j--;
						}
					} catch (NumberFormatException ex){}
				}
			}
		}

		// Remove terms that have no factors
		for (int i = 0; i < this.pieces.size(); i++){
			EquationPiece piece = this.pieces.get(i);
			if (piece.getChildren().size() == 0){
				this.pieces.remove(i);
				i--;
			}
		}

		// Rewrite terms where the first factor is negative
		for (EquationPiece piece : this.pieces){
			if (piece.getChildren().get(0).prefix.equals("-")){
				piece.prefix = piece.prefix.equals("-") ? "+" : "-";
				piece.getChildren().get(0).prefix = "+";
			}
		}

		// If the first factor of a term has a division symbol, add '1/'
		for (EquationPiece piece : this.pieces){
			if (piece.getChildren().get(0).prefix.equals("/")){
				Factor newFactor = new Factor(piece, this.left);
				newFactor.setContent("1");
				piece.getChildren().add(0, newFactor);
			}
		}

		// Remove useless parenthesis
		int size = this.pieces.size();
		for (int i = 0; i < size; i++){
			EquationPiece piece = this.pieces.get(i);
			if (piece.getChildren().size() == 1){
				Factor f = (Factor)piece.getChildren().get(0);
				if (f.getExpression() != null && f.getExponent() == null){
					for (EquationPiece t : f.getExpression().getChildren()){
						t.setParent(this);
						this.pieces.add(t);
					}
					this.pieces.remove(i);
					i--;
					size--;
				}
			}
		}

		applyToChildren(expr -> expr.rewrite());
	}

	public void group(String prefix, Factor... gFactors){
		List<Factor> groupingFactors = Arrays.asList(gFactors);
		List<EquationPiece> common = new ArrayList<>();
		for (EquationPiece p : this.pieces){
			boolean all = true;
			for (Factor f : groupingFactors){
				if (!p.getChildren().contains(f)){
					all = false;
					break;
				}
			}
			if (all && p.getChildren().stream().filter(c -> groupingFactors.contains(c) && (((c.prefix.equals("+") || c.prefix.equals("-")) && prefix.equals("*")) || c.prefix.equals(prefix))).findAny().isPresent()){
				common.add(p);
			}
		}

		//System.out.format("Grouping: %40s\t%s\t%d\t%s\t%s\n", groupingFactors.stream().map(f -> f.getString(true)).toList(), prefix, common.size(), getString(true), common.stream().map(c -> c.getString(true)).toList());

		if (common.size() <= 1) return;
		Term newTerm = new Term(this, this.left);
		Factor coef = new Factor(newTerm, this.left);
		Expression exp = new Expression(coef, this.left);
		newTerm.getChildren().add(coef);
		for (int i = 0; i < groupingFactors.size(); i++){
			Factor gFactor = groupingFactors.get(i);
			Factor commonFactor = (Factor)gFactor.copy(newTerm);
			if (i == 0) commonFactor.prefix = prefix;
			newTerm.getChildren().add(commonFactor);
		}
		coef.getChildren().add(exp);
		for (EquationPiece p : common){
			Term nt = new Term(exp, this.left);
			nt.prefix = p.prefix;
			for (EquationPiece f : p.getChildren()){
				if (!groupingFactors.contains(f)){
					nt.getChildren().add(f);
				}
			}
			if (nt.getChildren().size() == 0){
				Factor c1 = new Factor(nt, this.left);
				c1.setContent("1");
				nt.getChildren().add(c1);
			} else {
				if (nt.getChildren().get(0).prefix.equals("*")){
					nt.getChildren().get(0).prefix = "+";
				}
			}
			exp.getChildren().add(nt);
			this.pieces.remove(p);
		}
		this.pieces.add(newTerm);

		//System.out.println("Result: "+getString(true));

		applyToChildren(expr -> expr.group(prefix, gFactors));
	}

	public void expand(){
		int size = this.pieces.size();
		for (int i = 0; i < size; i++){
			EquationPiece piece = this.pieces.get(i);
			if (piece.getChildren().size() >= 2){
				Factor toMultiply = null;
				Factor multiplied = null;
				int pos1 = -1;
				int pos2 = -1;
				for (int j = 0; j < piece.getChildren().size(); j++){
					Factor factor = (Factor)piece.getChildren().get(j);
					if (factor.getExpression() != null && toMultiply == null){
						toMultiply = factor;
						pos1 = j;
					} else {
						multiplied = factor;
						pos2 = j;
					}
					if (toMultiply != null && multiplied != null) break;
				}

				if (toMultiply != null && multiplied != null){
					toMultiply.multiply(multiplied, pos1 < pos2);
					piece.getChildren().remove(multiplied);
					if (pos2 == 0){
						toMultiply.prefix = "+";
					}
					if (piece.getChildren().size() == 1){ // There is only 1 expression now
						Expression exp = ((Factor)piece.getChildren().get(0)).getExpression();
						this.pieces.remove(i);
						i--;
						size--;
						for (EquationPiece p : exp.getChildren()){
							if (piece.prefix.equals("-")){ // TODO: Check if also the expression has a - (?)
								((Term)p).changeSign();
							}
							p.setParent(piece.getParent());
							this.pieces.add(p);
						}
					}
				}
			}
		}

		applyToChildren(expr -> expr.expand());
	}

	public void removeMinus(){
		int size = this.pieces.size();
		for (int i = 0; i < size; i++){
			EquationPiece piece = this.pieces.get(i);
			for (EquationPiece p : piece.getChildren()){
				Factor f = (Factor)p;
				if (f.getExpression() != null && f.prefix.equals("-") && f.getExponent() == null){
					f.prefix = "+";
					for (EquationPiece t : f.getExpression().getChildren()){
						((Term)t).changeSign();
					}

					if (piece.getChildren().size() == 1){
						for (EquationPiece t : f.getExpression().getChildren()){
							t.setParent(this);
							this.pieces.add(t);
						}
						this.pieces.remove(i);
						i--;
						size--;
					}
				}
			}
		}

		applyToChildren(expr -> expr.removeMinus());
	}

	private void applyToChildren(Consumer<Expression> consumer){
		final int size = this.pieces.size();
		for (int i = 0; i < size; i++){
			EquationPiece piece = this.pieces.get(i);
			for (EquationPiece f : piece.getChildren()){
				Factor factor = (Factor)f;
				Expression expr = factor.getExpression();
				Expression arg = factor.getArgument();
				Factor exp = factor.getExponent();
				if (expr != null){
					consumer.accept(expr);
				}
				if (arg != null){
					consumer.accept(arg);
				}
				if (exp != null && exp.getExpression() != null){
					consumer.accept(exp.getExpression());
				}
			}
		}
	}

	@Override
	public EquationPiece copy(EquationPiece parent){
		Expression exp = new Expression(parent, this.left);
		for (EquationPiece piece : this.pieces){
			exp.getChildren().add(piece.copy(exp));
		}
		return exp;
	}

	@Override
	public String getString(boolean pref){
		StringBuilder builder = new StringBuilder();
		if (this.parent != null) builder.append("(");
		for (int i = 0; i < this.pieces.size(); i++){
			EquationPiece piece = this.pieces.get(i);
			builder.append(piece.getString(i > 0));
		}
		if (this.parent != null) builder.append(")");
		return builder.toString();
	}

	@Override
	public String print(int depth){
		StringBuilder builder = new StringBuilder();
		if (this.parent == null) builder.append("NULL parent\n");
		builder.append("\t".repeat(depth)+"Expression ["+(this.prefix == null ? "" : this.prefix)+"]\n");
		for (EquationPiece p : this.pieces){
			builder.append(p.print(depth+1)).append("\n");
		}

		return builder.toString();
	}
}