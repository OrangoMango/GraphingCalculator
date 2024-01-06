package com.orangomango.graphcalc.math;

import java.util.Objects;

public class Factor extends EquationPiece{
	private String content;
	private Expression argument;
	private Factor exponent;

	public Factor(EquationPiece parent, boolean left){
		super(parent, left);
	}

	public void multiply(Factor factor, boolean append){
		Expression expression = getExpression();
		if (expression == null){
			throw new IllegalStateException("This factor does not contain an expression");
		} else {
			for (int i = 0; i < expression.getChildren().size(); i++){
				EquationPiece p = expression.getChildren().get(i);
				if (append){
					p.getChildren().add(factor.copy(p));
				} else {
					EquationPiece newFactor = factor.copy(p);
					boolean addOne = newFactor.prefix.equals("/");
					if (p.getChildren().get(0).prefix.equals("-")){
						newFactor.prefix = factor.prefix.equals("-") ? "+" : "-";
					} else {
						newFactor.prefix = "+";
					}
					p.getChildren().get(0).prefix = this.prefix;
					p.getChildren().add(0, newFactor);
					if (addOne){
						Factor one = new Factor(p, this.left);
						one.setContent("1");
						one.prefix = newFactor.prefix;
						newFactor.prefix = "/";
						p.getChildren().add(0, one);
					}
				}
			}
		}
	}

	@Override
	public boolean equals(Object other){
		if (other instanceof Factor f){
			if (!Objects.equals(getExponent(), f.getExponent())) return false;
			if (!Objects.equals(getArgument(), f.getArgument())) return false;
			if (!Objects.equals(getExpression(), f.getExpression())) return false;
			if (!Objects.equals(getContent(), f.getContent())) return false;
			return true;
		} else return false;
	}

	@Override
	public EquationPiece copy(EquationPiece parent){
		Factor factor = new Factor(parent, this.left);
		factor.prefix = this.prefix;
		factor.setContent(this.content);
		if (getExpression() != null) factor.getChildren().add(getExpression().copy(factor));
		if (this.exponent != null) factor.setExponent((Factor)this.exponent.copy(factor));
		if (this.argument != null) factor.setArgument((Expression)this.argument.copy(factor));
		return factor;
	}

	@Override
	public String getString(boolean pref){
		StringBuilder builder = new StringBuilder();
		if (pref || !this.prefix.equals("+")) builder.append(this.prefix);
		if (getExpression() != null){
			builder.append(getExpression().getString(true));
		} else {
			builder.append(this.content);
		}
		if (this.argument != null){
			builder.append(this.argument.getString(true));
		}
		if (this.exponent != null){
			builder.append("^");
			builder.append(this.exponent.getString(false));
		}
		return builder.toString();
	}

	@Override
	public String print(int depth){
		StringBuilder builder = new StringBuilder();
		if (this.parent == null) builder.append("NULL parent\n");
		if (this.pieces.size() == 0){
			builder.append("\t".repeat(depth)+"["+(this.prefix == null ? "" : this.prefix)+"] "+this.content+" -> "+(this.left ? "L" : "R"));
			if (this.argument != null){
				builder.append("\n");
				builder.append("\t".repeat(depth)+"Argument: \n");
				builder.append(this.argument.print(depth+1));
			}
		} else {
			builder.append("\t".repeat(depth)+"["+(this.prefix == null ? "" : this.prefix)+"]:\n"+this.pieces.get(0).print(depth));
		}
		if (this.exponent != null){
			builder.append("\n");
			builder.append("\t".repeat(depth)+"Exponent: \n");
			builder.append(this.exponent.print(depth+1));
		}

		return builder.toString();
	}

	public void setContent(String text){
		this.content = text;
	}

	public String getContent(){
		return this.content;
	}

	public void setArgument(Expression text){
		this.argument = text;
	}

	public Expression getArgument(){
		return this.argument;
	}

	public void setExponent(Factor f){
		this.exponent = f;
	}

	public Factor getExponent(){
		return this.exponent;
	}

	public Expression getExpression(){
		if (this.pieces.size() == 0){
			return null;
		} else {
			return (Expression)this.pieces.get(0);
		}
	}
}