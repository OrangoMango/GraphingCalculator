package com.orangomango.graphcalc.math;

public class Term extends EquationPiece{
	public Term(EquationPiece parent, boolean left){
		super(parent, left);
	}

	public void changeSign(){
		if (this.prefix.equals("+")){
			this.prefix = "-";
		} else {
			this.prefix = "+";
		}
	}

	@Override
	public EquationPiece copy(EquationPiece parent){
		Term term = new Term(parent, this.left);
		term.prefix = this.prefix;
		for (EquationPiece piece : this.pieces){
			term.getChildren().add(piece.copy(term));
		}
		return term;
	}

	@Override
	public String getString(boolean pref){
		StringBuilder builder = new StringBuilder();
		if (pref || !this.prefix.equals("+")) builder.append(this.prefix);
		for (int i = 0; i < this.pieces.size(); i++){
			EquationPiece piece = this.pieces.get(i);
			builder.append(piece.getString(i > 0));
		}
		return builder.toString();
	}

	@Override
	public String print(int depth){
		StringBuilder builder = new StringBuilder();
		if (this.parent == null) builder.append("NULL parent\n");
		builder.append("\t".repeat(depth)+"Term ["+(this.prefix == null ? "" : this.prefix)+"]\n");
		for (EquationPiece p : this.pieces){
			builder.append(p.print(depth+1)).append("\n");
		}

		return builder.toString();
	}
}