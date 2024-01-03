package com.orangomango.graphcalc.math;

import java.util.*;
import java.util.function.Consumer;

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

	}

	public void group(String factorName){
		List<EquationPiece> common = new ArrayList<>();
		for (EquationPiece p : this.pieces){
			if (p.getChildren().stream().map(c -> ((Factor)c).getContent()).filter(c -> c.equals(factorName)).findAny().isPresent()){
				common.add(p);
			}
		}
		if (common.size() <= 1) return;
		Term newTerm = new Term(this.parent, this.left);
		Factor commonFactor = new Factor(newTerm, this.left);
		commonFactor.setContent(factorName);
		commonFactor.prefix = "*";
		Factor coef = new Factor(newTerm, this.left);
		Expression exp = new Expression(coef, this.left);
		newTerm.getChildren().add(coef);
		newTerm.getChildren().add(commonFactor);
		coef.getChildren().add(exp);
		for (EquationPiece p : common){
			Term nt = new Term(exp, this.left);
			nt.prefix = p.prefix;
			for (EquationPiece f : p.getChildren()){
				Factor factor = (Factor)f;
				if (!factor.getContent().equals(factorName)){
					nt.getChildren().add(factor);
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

		applyToChildren(expr -> expr.group(factorName));
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
					if (piece.getChildren().size() == 1 && piece.prefix.equals("+")){ // There is only 1 expression now
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