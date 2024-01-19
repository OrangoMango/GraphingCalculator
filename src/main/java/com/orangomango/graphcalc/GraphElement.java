package com.orangomango.graphcalc;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Map;

public abstract class GraphElement{
	protected Color color;

	public GraphElement(Color color){
		this.color = color;
	}

	public abstract void render(GraphicsContext gc, double topPos, double bottomPos, double scaleFactor);
	public abstract void edit(String f, Map<String, Double> params);

	public Color getColor(){
		return this.color;
	}

	public void setColor(Color color){
		this.color = color;
	}
}