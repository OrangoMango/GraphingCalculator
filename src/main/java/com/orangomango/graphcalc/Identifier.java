package com.orangomango.graphcalc;

public enum Identifier{
	GRAPH_FUNCTION("GF"), GRAPH_POINT("GP"), GRAPH_LINE("GL");

	private String id;

	private Identifier(String id){
		this.id = id;
	}

	public static Identifier parse(String text){
		for (Identifier i : Identifier.values()){
			if (i.getId().equals(text)){
				return i;
			}
		}

		return null;
	}

	public String getId(){
		return this.id;
	}
}