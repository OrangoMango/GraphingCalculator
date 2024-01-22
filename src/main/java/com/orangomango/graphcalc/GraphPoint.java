package com.orangomango.graphcalc;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.geometry.Point2D;

import java.util.*;
import java.text.DecimalFormat;

import com.orangomango.graphcalc.math.Evaluator;

// TODO: Parametric point
public class GraphPoint extends GraphElement implements Transformable{
	private Point2D position;
	private String name;

	public GraphPoint(Color color, String pos){
		super(color);
		edit(pos, null);
	}

	private Point2D getCoords(String pos){
		pos = pos.substring(0, pos.length()-1);
		Evaluator eval = new Evaluator(pos.split(",")[0].trim(), null);
		double x = eval.parse();
		eval = new Evaluator(pos.split(",")[1].trim(), null);
		double y = eval.parse();
		return new Point2D(x, y);
	}

	@Override
	public GraphElement transform(Color color, String xEq, String yEq){
		Map<String, Double> params = new HashMap<>();
		params.put("x", this.position.getX());
		params.put("y", this.position.getY());
		Evaluator eval = new Evaluator(xEq, params);
		double tx = eval.parse();
		eval = new Evaluator(yEq, params);
		double ty = eval.parse();
		return new GraphPoint(color, String.format("%s'(%s,%s)", this.name, tx, ty));
	}

	public String getName(){
		return this.name;
	}

	public Point2D getPosition(){
		return this.position;
	}

	@Override
	// TODO: implement with params
	public void edit(String f, Map<String, Double> params){
		this.name = f.split("\\(")[0];
		if (name.isBlank()){
			throw new IllegalArgumentException("Name is empty");
		}
		this.position = getCoords(f.split("\\(", 2)[1]);
	}

	@Override
	public void render(GraphicsContext gc, double topPos, double bottomPos, double scaleFactor){
		gc.setFill(this.color);
		final double x = MainApplication.WIDTH/2+this.position.getX()*scaleFactor;
		final double y = MainApplication.HEIGHT/2-this.position.getY()*scaleFactor;
		gc.fillOval(x-5, y-5, 10, 10);
		gc.setFont(MainApplication.FONT);
		gc.setTextAlign(TextAlignment.LEFT);
		gc.fillText(this.name, x+7, y+2);
	}

	@Override
	public String toString(){
		DecimalFormat formatter = new DecimalFormat("#.###");
		return String.format("%s(%s, %s)", this.name, formatter.format(this.position.getX()), formatter.format(this.position.getY()));
	}
}