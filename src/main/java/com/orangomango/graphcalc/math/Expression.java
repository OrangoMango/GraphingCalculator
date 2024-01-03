package com.orangomango.graphcalc.math;

import java.util.*;

public class Expression extends EquationPiece{
	public Expression(EquationPiece parent, boolean left){
		super(parent, left);
	}

	public void add(Expression expression){
		for (EquationPiece piece : expression.getChildren()){
			this.pieces.add(piece);
		}
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
							piece.getParent().getChildren().add(p);
						}
					}
				}
			}

			for (EquationPiece f : piece.getChildren()){
				Factor factor = (Factor)f;
				Expression exp = factor.getExpression();
				if (exp != null) exp.reduce();
			}
		}

		for (EquationPiece piece : toRemove){
			this.pieces.remove(piece);
		}
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