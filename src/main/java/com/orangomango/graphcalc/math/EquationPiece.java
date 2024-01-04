package com.orangomango.graphcalc.math;

import java.util.*;

public abstract class EquationPiece{
	protected boolean left;
	protected List<EquationPiece> pieces = new ArrayList<>();
	protected EquationPiece parent;
	public String prefix = "+";

	public EquationPiece(EquationPiece parent, boolean left){
		this.parent = parent;
		this.left = left;
	}

	public List<EquationPiece> getChildren(){
		return this.pieces;
	}

	public boolean getSide(){
		return this.left;
	}

	public EquationPiece getParent(){
		return this.parent;
	}

	public void setParent(EquationPiece p){
		this.parent = p;
	}

	public void moveAll(boolean left){
		this.left = left;
		for (EquationPiece p : this.pieces){
			p.moveAll(left);
		}
	}

	public abstract EquationPiece copy(EquationPiece parent);
	public abstract String getString(boolean pref);
	public abstract String print(int depth);

	@Override
	public boolean equals(Object other){
		if (other instanceof EquationPiece ep){
			for (EquationPiece piece : this.pieces){
				if (!ep.getChildren().contains(piece)){
					return false;
				}
			}
			for (EquationPiece piece : ep.getChildren()){
				if (!this.pieces.contains(piece)){
					return false;
				}
			}
			return true;
		} else return false;
	}

	@Override
	public String toString(){
		return print(0);
	}
}