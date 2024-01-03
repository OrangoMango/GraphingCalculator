package com.orangomango.graphcalc.math;

public class Factor extends EquationPiece{
	private String content;
	private Expression argument;
	private Factor exponent;

	public Factor(EquationPiece parent, boolean left){
		super(parent, left);
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

	public void multiply(Factor factor, boolean append){
		Expression expression = getExpression();
		if (expression == null){
			throw new IllegalStateException("This factor does not contain an expression");
		} else {
			for (EquationPiece p : expression.getChildren()){
				if (append){
					p.getChildren().add(factor);
				} else {
					p.getChildren().add(0, factor);
				}
			}
		}
	}

	@Override
	public String getString(boolean pref){
		StringBuilder builder = new StringBuilder();
		if (pref) builder.append(this.prefix);
		if (this.pieces.size() == 0){
			builder.append(this.content);
		} else {
			builder.append(this.pieces.get(0).getString(true));
		}
		if (this.exponent != null){
			builder.append("^");
			builder.append(this.exponent.getString(true));
		}
		return builder.toString();
	}

	@Override
	public String print(int depth){
		StringBuilder builder = new StringBuilder();
		if (this.pieces.size() == 0){
			builder.append("\t".repeat(depth)+"["+(this.prefix == null ? "" : this.prefix)+"] "+this.content+" -> "+(this.left ? "L" : "R"));
			if (this.argument != null){
				builder.append("\n");
				builder.append("\t".repeat(depth)+"Argument: \n");
				builder.append(this.argument.print(depth+1));
			} else if (this.exponent != null){
				builder.append("\n");
				builder.append("\t".repeat(depth)+"Exponent: \n");
				builder.append(this.exponent.print(depth+1));
			}
		} else {
			builder.append(this.pieces.get(0).print(depth));
		}

		return builder.toString();
	}
}