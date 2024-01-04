package com.orangomango.graphcalc.math;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.orangomango.graphcalc.Evaluator;

public class Expression extends EquationPiece{
	public Expression(EquationPiece parent, boolean left){
		super(parent, left);
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

	public boolean canBeReduced(){
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
		for (EquationPiece piece : this.pieces){
			for (EquationPiece f : piece.getChildren()){
				Factor factor = (Factor)f;
				Expression exp = factor.getExpression();
				if (exp != null){
					try {
						Evaluator eval = new Evaluator(exp.getString(true), params);
						double value = eval.parse();
						factor.getChildren().clear();
						if (value < 0){
							factor.prefix = "-";
						}
						factor.setContent(Double.toString(Math.abs(value)));
					} catch (RuntimeException ex){}
				}
			}
		}

		applyToChildren(expr -> expr.calculate(params));
	}

	public void rewrite(){
		// Rewrite with exponents, x*x becomes x^2
		for (EquationPiece piece : this.pieces){
			Map<String, Integer> count = new HashMap<>();
			for (EquationPiece p : piece.getChildren()){
				String disp = p.getString(true).substring(1);
				count.put(disp, count.getOrDefault(disp, 0)+1);
			}
			for (Map.Entry<String, Integer> entry : count.entrySet()){
				if (entry.getValue() > 1){
					List<EquationPiece> toRemove = new ArrayList<>();
					boolean first = true;
					for (EquationPiece p : piece.getChildren()){
						if (p.getString(true).substring(1).equals(entry.getKey())){
							if (first){
								Factor factor = (Factor)p;
								Factor exp = new Factor(factor, this.left);
								exp.setContent(Integer.toString(entry.getValue()));
								factor.setExponent(exp);
								first = false;
							} else {
								toRemove.add(p);
							}
						}
					}
					for (EquationPiece p : toRemove){
						piece.getChildren().remove(p);
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
							int times = Integer.parseInt(factor.getExponent().getContent()); // TODO: ^0 = 1
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

		for (EquationPiece piece : this.pieces){
			if (piece.getChildren().size() == 1){
				Factor f = (Factor)piece.getChildren().get(0);
				// Remove factors that have content '0'
				if (f.getContent() != null && f.getContent().equals("0")){ // TODO: Improve
					piece.getChildren().remove(f);
				}
			} else {
				List<EquationPiece> toRemove = new ArrayList<>();
				for (EquationPiece p : piece.getChildren()){
					// Remove factors that have a term-index > 0 and content = '1'
					if (((Factor)p).getContent() != null && ((Factor)p).getContent().equals("1") && (p.prefix.equals("*") || p.prefix.equals("/"))){
						toRemove.add(p);
					}
				}
				for (EquationPiece p : toRemove){
					piece.getChildren().remove(p);
				}
			}
		}

		// Remove terms that have no factors
		List<EquationPiece> toRemove = new ArrayList<>();
		for (EquationPiece piece : this.pieces){
			if (piece.getChildren().size() == 0){
				toRemove.add(piece);
			}
		}
		for (EquationPiece p : toRemove){
			this.pieces.remove(p);
		}
		toRemove.clear();

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
		final int size = this.pieces.size();
		for (int i = 0; i < size; i++){
			EquationPiece piece = this.pieces.get(i);
			if (piece.getChildren().size() == 1){
				Factor f = (Factor)piece.getChildren().get(0);
				if (f.getExpression() != null && f.getExponent() == null){
					for (EquationPiece t : f.getExpression().getChildren()){
						t.setParent(this);
						this.pieces.add(t);
					}
					toRemove.add(piece);
				}
			}
		}

		for (EquationPiece p : toRemove){
			this.pieces.remove(p);
		}

		applyToChildren(expr -> expr.rewrite());
	}

	public void group(Factor groupingFactor, String prefix){
		List<EquationPiece> common = new ArrayList<>();
		Predicate<EquationPiece> acceptable = c -> c.equals(groupingFactor) && (((c.prefix.equals("+") || c.prefix.equals("-")) && prefix.equals("*")) || c.prefix.equals(prefix));
		for (EquationPiece p : this.pieces){
			if (p.getChildren().stream().filter(acceptable).findAny().isPresent()){
				common.add(p);
			}
		}

		if (common.size() <= 1) return;
		Term newTerm = new Term(this, this.left);
		Factor commonFactor = (Factor)groupingFactor.copy(newTerm);
		commonFactor.prefix = prefix;
		Factor coef = new Factor(newTerm, this.left);
		Expression exp = new Expression(coef, this.left);
		newTerm.getChildren().add(coef);
		newTerm.getChildren().add(commonFactor);
		coef.getChildren().add(exp);
		for (EquationPiece p : common){
			Term nt = new Term(exp, this.left);
			nt.prefix = p.prefix;
			for (EquationPiece f : p.getChildren()){
				if (!acceptable.test(f)){
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

		applyToChildren(expr -> expr.group(groupingFactor, prefix));
	}

	public void reduce(){
		List<EquationPiece> toRemove = new ArrayList<>();
		final int size = this.pieces.size();
		for (int i = 0; i < size; i++){
			EquationPiece piece = this.pieces.get(i);
			if (piece.getChildren().size() >= 2){
				Factor toMultiply = null;
				Factor multiplied = null;
				for (EquationPiece f : piece.getChildren()){
					Factor factor = (Factor)f;
					if (factor.getExpression() != null && toMultiply == null){
						toMultiply = factor;
					} else {
						multiplied = factor;
					}
					if (toMultiply != null && multiplied != null) break;
				}

				if (toMultiply != null && multiplied != null){
					toMultiply.multiply(multiplied, piece.getChildren().indexOf(toMultiply) < piece.getChildren().indexOf(multiplied));		
					piece.getChildren().remove(multiplied);
					if (piece.getChildren().indexOf(toMultiply) == 0){
						toMultiply.prefix = "+";
					}
				}
			}
		}

		for (EquationPiece piece : toRemove){
			this.pieces.remove(piece);
		}

		applyToChildren(expr -> expr.reduce());
	}

	public void removeMinus(){
		final int size = this.pieces.size();
		for (int i = 0; i < size; i++){
			EquationPiece piece = this.pieces.get(i);
			EquationPiece toRemove = null;
			for (EquationPiece p : piece.getChildren()){
				Factor f = (Factor)p;
				if (f.getExpression() != null && f.prefix.equals("-")){
					f.prefix = "+";
					for (EquationPiece t : f.getExpression().getChildren()){
						((Term)t).changeSign();
					}

					if (piece.getChildren().size() == 1){
						for (EquationPiece t : f.getExpression().getChildren()){
							t.setParent(this);
							this.pieces.add(t);
						}
						toRemove = piece;
					}
				}
			}
			if (toRemove != null){
				this.pieces.remove(toRemove);
			}
		}

		applyToChildren(expr -> expr.removeMinus());
	}

	private void applyToChildren(Consumer<Expression> consumer){
		final int size = this.pieces.size(); // TODO: Fix concurrentModification while deleting (?)
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