package com.orangomango.graphcalc;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Map;

public class GraphLine extends GraphElement{
	private GraphPoint start, end;

	public GraphLine(Color color, GraphPoint start, GraphPoint end){
		super(color);
		this.start = start;
		this.end = end;
	}

	@Override
	public void edit(String f, Map<String, Double> params){
		// TODO
	}

	@Override
	public void render(GraphicsContext gc, double topPos, double bottomPos, double scaleFactor){
		gc.setStroke(this.color);
		final double sx = MainApplication.WIDTH/2+this.start.getPosition().getX()*scaleFactor;
		final double sy = MainApplication.HEIGHT/2-this.start.getPosition().getY()*scaleFactor;
		final double ex = MainApplication.WIDTH/2+this.end.getPosition().getX()*scaleFactor;
		final double ey = MainApplication.HEIGHT/2-this.end.getPosition().getY()*scaleFactor;
		gc.strokeLine(sx, sy, ex, ey);
	}

	@Override
	public String toString(){
		return String.format("%s <-> %s", this.start, this.end);
	}
}