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

	public abstract String getString(boolean pref);
	public abstract String print(int depth);

	@Override
	public String toString(){
		return print(0);
	}
}