package com.orangomango.graphcalc;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.*;

public class GraphLine extends GraphElement{
	private List<GraphPoint> points = new ArrayList<>();
	private List<GraphElement> elements;

	public GraphLine(Color color, String def, List<GraphElement> elements){
		super(color);
		this.elements = elements;
		edit(def, null);
	}

	@Override
	// TODO: implement with params
	public void edit(String f, Map<String, Double> params){
		final String data = f.substring(5, f.length()-1);
		this.points.clear();
		for (String name : data.split(",")){
			GraphPoint point = (GraphPoint)this.elements.stream().filter(ele -> ele instanceof GraphPoint && ((GraphPoint)ele).getName().equals(name.trim())).findAny().orElse(null);
			this.points.add(point);
		}
	}

	@Override
	public void render(GraphicsContext gc, double topPos, double bottomPos, double scaleFactor){
		gc.setStroke(this.color);
		for (int i = 0; i < this.points.size()-1; i++){
			final double sx = MainApplication.WIDTH/2+this.points.get(i).getPosition().getX()*scaleFactor;
			final double sy = MainApplication.HEIGHT/2-this.points.get(i).getPosition().getY()*scaleFactor;
			final double ex = MainApplication.WIDTH/2+this.points.get(i+1).getPosition().getX()*scaleFactor;
			final double ey = MainApplication.HEIGHT/2-this.points.get(i+1).getPosition().getY()*scaleFactor;
			gc.strokeLine(sx, sy, ex, ey);
		}
	}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("LINE(");
		for (int i = 0; i < this.points.size(); i++){
			builder.append(this.points.get(i).getName());
			if (i < this.points.size()-1){
				builder.append(", ");
			}
		}
		builder.append(")");

		return builder.toString();
	}
}