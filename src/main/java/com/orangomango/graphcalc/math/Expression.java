package com.orangomango.graphcalc.math;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Expression extends EquationPiece{
	public Expression(EquationPiece parent, boolean left){
		super(parent, left);
	}

	public void add(Expression expression){
		for (EquationPiece piece : expression.getChildren()){
			this.pieces.add(piece);
		}
	}

	public boolean canBeReduced(){
		for (EquationPiece piece : this.pieces){
			if (piece.getChildren().size() >= 2){
				boolean found1 = false;
				boolean found2 = false;
				for (EquationPiece f : piece.getChildren()){
					Factor factor = (Factor)f;
					if (factor.getExpression() != null){
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

	public void rewrite(){
		// Rewrite with exponents, x*x becomes x^2
		for (EquationPiece piece : this.pieces){
			Map<String, Integer> count = new HashMap<>();
			for (EquationPiece p : piece.getChildren()){
				String disp = p.getString(true);
				count.put(disp, count.getOrDefault(disp, 0)+1);
			}
			for (Map.Entry<String, Integer> entry : count.entrySet()){
				if (entry.getValue() > 1){
					int rmCount = 0;
					List<EquationPiece> toRemove = new ArrayList<>();
					EquationPiece remaining = null;
					for (EquationPiece p : piece.getChildren()){
						if (p.getString(true).equals(entry.getKey())){
							if (rmCount < entry.getValue()-1){
								toRemove.add(p);
								rmCount++;
							} else {
								remaining = p;
							}
						}
					}
					for (EquationPiece p : toRemove){
						piece.getChildren().remove(p);
					}
					Factor factor = (Factor)remaining;
					Factor exp = new Factor(factor, this.left);
					exp.setContent(Integer.toString(entry.getValue()));
					factor.setExponent(exp);
				}
			}
		}

		for (EquationPiece piece : this.pieces){
			if (piece.getChildren().size() == 1){
				Factor f = (Factor)piece.getChildren().get(0);
				// Remove factors that have content '0'
				if (f.getContent().equals("0")){
					piece.getChildren().remove(f);
				}
			} else {
				List<EquationPiece> toRemove = new ArrayList<>();
				for (EquationPiece p : piece.getChildren()){
					// Remove factors that have a term-index > 0 and content = '1'
					if (((Factor)p).getContent().equals("1") && (p.prefix.equals("*") || p.prefix.equals("/"))){
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

		// Rewrite negative terms where the first factor is negative
		for (EquationPiece piece : this.pieces){
			if (piece.prefix.equals("-") && piece.getChildren().get(0).prefix.equals("-")){
				piece.prefix = "+";
				piece.getChildren().get(0).prefix = "+";
			}
		}

		applyToChildren(expr -> expr.rewrite());
	}

	public void group(String factorName, String prefix){
		List<EquationPiece> common = new ArrayList<>();
		Predicate<EquationPiece> acceptable = c -> c.getString(true).substring(1).equals(factorName) && (((c.prefix.equals("+") || c.prefix.equals("-")) && prefix.equals("*")) || c.prefix.equals(prefix));
		for (EquationPiece p : this.pieces){
			if (p.getChildren().stream().filter(acceptable).findAny().isPresent()){
				common.add(p);
			}
		}
		if (common.size() <= 1) return;
		Term newTerm = new Term(this.parent, this.left);
		Factor commonFactor = new Factor(newTerm, this.left);
		commonFactor.setContent(factorName);
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
				if (nt.getChildren().size() == 0){
					Factor c1 = new Factor(nt, this.left);
					c1.setContent("1");
					nt.getChildren().add(c1);
				}
			}
			exp.getChildren().add(nt);
			this.pieces.remove(p);
		}
		this.pieces.add(newTerm);

		applyToChildren(expr -> expr.group(factorName, prefix));
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
					if (factor.getExpression() != null){
						toMultiply = factor;
					} else {
						multiplied = factor;
					}
					if (toMultiply != null && multiplied != null) break;
				}

				if (toMultiply != null && multiplied != null){
					toMultiply.multiply(multiplied, piece.getChildren().indexOf(toMultiply) < piece.getChildren().indexOf(multiplied));
					piece.getChildren().remove(multiplied);
					if (piece.getChildren().size() == 1){ // There is only 1 expression now
						Expression exp = ((Factor)piece.getChildren().get(0)).getExpression();
						toRemove.add(piece);
						for (EquationPiece p : exp.getChildren()){
							p.setParent(piece.getParent());
							piece.getParent().getChildren().add(p);
						}
					}
				}
			}
		}

		applyToChildren(expr -> expr.reduce());

		for (EquationPiece piece : toRemove){
			this.pieces.remove(piece);
		}
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
		for (EquationPiece piece : this.pieces){
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
		builder.append("\t".repeat(depth)+"Expression ["+(this.prefix == null ? "" : this.prefix)+"]\n");
		for (EquationPiece p : this.pieces){
			builder.append(p.print(depth+1)).append("\n");
		}

		return builder.toString();
	}
}