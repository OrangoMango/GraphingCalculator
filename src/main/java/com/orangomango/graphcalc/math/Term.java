package com.orangomango.graphcalc.math;

public class Term extends EquationPiece{
	public Term(EquationPiece parent, boolean left){
		super(parent, left);
	}

	public void changeSign(){
		if (this.prefix == null || this.prefix.equals("+")){
			this.prefix = "-";
		} else {
			this.prefix = "+";
		}
	}

	@Override
	public String getString(boolean pref){
		StringBuilder builder = new StringBuilder();
		if (pref) builder.append(this.prefix);
		for (int i = 0; i < this.pieces.size(); i++){
			EquationPiece piece = this.pieces.get(i);
			builder.append(piece.getString(i > 0));
		}
		return builder.toString();
	}

	@Override
	public String print(int depth){
		StringBuilder builder = new StringBuilder();
		builder.append("\t".repeat(depth)+"Term ["+(this.prefix == null ? "" : this.prefix)+"]\n");
		for (EquationPiece p : this.pieces){
			builder.append(p.print(depth+1)).append("\n");
		}

		return builder.toString();
	}
}