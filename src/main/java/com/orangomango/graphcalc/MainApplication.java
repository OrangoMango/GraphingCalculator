package com.orangomango.graphcalc;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import javafx.util.Pair;

import java.util.*;

public class MainApplication extends Application{
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int FPS = 40;

	private HashMap<KeyCode, Boolean> keys = new HashMap<>();
	private int frames, fps;
	private List<GraphFunction> functions = new ArrayList<>();
	private double cameraX, cameraY;
	private double scaleFactor = 40;

	// DEBUG
	private double xSample = 0;
	private double calcDelta = 0.01;

	@Override
	public void start(Stage stage){
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		pane.getChildren().add(canvas);
		GraphicsContext gc = canvas.getGraphicsContext2D();

		canvas.setFocusTraversable(true);
		canvas.setOnKeyPressed(e -> this.keys.put(e.getCode(), true));
		canvas.setOnKeyReleased(e -> this.keys.put(e.getCode(), false));

		canvas.setOnScroll(e -> {
			if (e.getDeltaY() > 0){
				this.scaleFactor += 0.5;
			} else if (e.getDeltaY() < 0){
				this.scaleFactor -= 0.5;
			}

			this.scaleFactor = Math.min(120, Math.max(this.scaleFactor, 20));
		});

		this.functions.add(new GraphFunction(Color.BLUE, x -> 5*x+2));
		this.functions.add(new GraphFunction(Color.RED, x -> Math.sqrt(25-x*x)));
		for (GraphFunction func : this.functions){
			func.buildInterval(-10, 10, 0.005, -10, 10);
		}

		List<Double> output = findIntersections(this.functions.get(0), this.functions.get(1));
		System.out.println(output);

		AnimationTimer timer = new AnimationTimer(){
			@Override
			public void handle(long time){
				update(gc);
				MainApplication.this.frames++;
				stage.setTitle("Graphical calculator - FPS:"+MainApplication.this.fps); // Set FPS title
			}
		};
		timer.start();

		Thread fpsCounter = new Thread(() -> {
			while (true){
				try {
					this.fps = this.frames;
					this.frames = 0;
					Thread.sleep(1000);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		fpsCounter.setDaemon(true);
		fpsCounter.start();

		Scene scene = new Scene(pane, WIDTH, HEIGHT);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();
	}

	private List<Double> findIntersections(GraphFunction f1, GraphFunction f2){
		List<Double> output = new ArrayList<>();

		for (double i = -10; i < 10; i += 0.001){
			double delta = Math.abs(f1.getDefinition().apply(i)-f2.getDefinition().apply(i));
			if (delta < this.calcDelta){
				//System.out.println("Potential solution: "+i);
				Double solution = findPoint(f1, f2, i, 0.001, 1);
				if (solution != null) output.add(solution);
			}
		}

		List<Double> temp = new ArrayList<>(output.stream().distinct().toList());
		List<Double> result = new ArrayList<>();
		for (int i = 0; i < temp.size(); i++){
			double sol = temp.get(i);
			double sum = sol;
			int n = 1;
			for (int j = i+1; j < temp.size(); j++){
				double sol2 = temp.get(j);
				if (Math.abs(sol-sol2) < 0.1){
					sum += sol2;
					n++;
				}
			}
			result.add(sum/n);
			i += n-1;
		}

		return result;
	}

	private Double findPoint(GraphFunction f1, GraphFunction f2, double value, double step, int depth){
		double delta1 = Math.abs(f1.getDefinition().apply(value-step/2)-f2.getDefinition().apply(value-step/2));
		double delta2 = Math.abs(f1.getDefinition().apply(value+step/2)-f2.getDefinition().apply(value+step/2));	

		if (depth == 25){
			if (Math.abs(delta1) < 0.001 && Math.abs(delta2) < 0.001){ // Must be almost 0
				//System.out.println("Reached depth 50 and the value is: "+value);
				//System.out.println("Deltas: "+delta1+" and "+delta2);
				return value;	
			} else return null;
		}

		if (delta1 < delta2){
			return findPoint(f1, f2, value-step/2, step/2, depth+1);
		} else if (delta1 > delta2){
			return findPoint(f1, f2, value+step/2, step/2, depth+1);
		} else {
			return findPoint(f1, f2, value-step/2, step/2, depth+1); // Pick a random way
		}
	}

	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#B1B1B1"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		final int cameraSpeed = 4;
		if (this.keys.getOrDefault(KeyCode.W, false)){
			this.cameraY -= cameraSpeed;
			this.keys.put(KeyCode.W, false);
		} else if (this.keys.getOrDefault(KeyCode.A, false)){
			this.cameraX -= cameraSpeed;
			this.keys.put(KeyCode.A, false);
		} else if (this.keys.getOrDefault(KeyCode.S, false)){
			this.cameraY += cameraSpeed;
			this.keys.put(KeyCode.S, false);
		} else if (this.keys.getOrDefault(KeyCode.D, false)){
			this.cameraX += cameraSpeed;
			this.keys.put(KeyCode.D, false);
		} else if (this.keys.getOrDefault(KeyCode.ESCAPE, false)){
			System.exit(0);
		}

		// DEBUG
		if (this.keys.getOrDefault(KeyCode.DIGIT1, false)){
			List<GraphFunction> temp = new ArrayList<>();
			temp.add(new GraphFunction(Color.BLUE, x -> 5/x*Math.sin(2*Math.PI*x)));
			temp.add(new GraphFunction(Color.RED, x -> x*x));
			this.functions = temp;
			for (GraphFunction func : this.functions){
				func.buildInterval(-10, 10, 0.005, -10, 10);
			}
			this.keys.put(KeyCode.DIGIT1, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT2, false)){
			List<GraphFunction> temp = new ArrayList<>();
			temp.add(new GraphFunction(Color.BLUE, x -> x*x));
			temp.add(new GraphFunction(Color.RED, x -> 2*x*x));
			this.functions = temp;
			for (GraphFunction func : this.functions){
				func.buildInterval(-10, 10, 0.005, -10, 10);
			}
			this.keys.put(KeyCode.DIGIT2, false);
		} else if (this.keys.getOrDefault(KeyCode.DIGIT3, false)){
			List<GraphFunction> temp = new ArrayList<>();
			temp.add(new GraphFunction(Color.BLUE, x -> Math.abs(Math.log(x))));
			temp.add(new GraphFunction(Color.RED, x -> 1-x*x));
			this.functions = temp;
			for (GraphFunction func : this.functions){
				func.buildInterval(-10, 10, 0.005, -10, 10);
			}
			this.keys.put(KeyCode.DIGIT3, false);
		}

		if (this.keys.getOrDefault(KeyCode.LEFT, false)){
			this.xSample -= 0.001;
			this.keys.put(KeyCode.LEFT, false);
		} else if (this.keys.getOrDefault(KeyCode.RIGHT, false)){
			this.xSample += 0.001;
			this.keys.put(KeyCode.RIGHT, false);
		} else if (this.keys.getOrDefault(KeyCode.SPACE, false)){
			List<Double> output = findIntersections(this.functions.get(0), this.functions.get(1));
			System.out.println("Solution: "+output);
			this.keys.put(KeyCode.SPACE, false);
		} else if (this.keys.getOrDefault(KeyCode.UP, false)){
			this.calcDelta *= 10;
			this.keys.put(KeyCode.UP, false);
		} else if (this.keys.getOrDefault(KeyCode.DOWN, false)){
			this.calcDelta /= 10;
			this.keys.put(KeyCode.DOWN, false);
		}

		/*gc.setFill(Color.GREEN);
		gc.setFont(new Font("sans-serif", 20));
		gc.setTextAlign(TextAlignment.LEFT);
		double value1 = this.functions.get(0).getDefinition().apply(this.xSample);
		double value2 = this.functions.get(1).getDefinition().apply(this.xSample);
		gc.fillText(String.format("xSample: %.15f\nf1: %.15f\nf2: %.15f\ndelta: %.15f\n\ncalcDelta: %.15f", this.xSample, value1, value2, Math.abs(value1-value2), this.calcDelta), 50, 50);*/

		gc.save();
		gc.translate(-this.cameraX, -this.cameraY);

		// Axes
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(3);
		gc.strokeLine(WIDTH/2, this.cameraY, WIDTH/2, this.cameraY+HEIGHT);
		gc.strokeLine(this.cameraX, HEIGHT/2, this.cameraX+WIDTH, HEIGHT/2);

		// Debug axis
		//gc.setStroke(Color.GREEN);
		//gc.setLineWidth(3);
		//gc.strokeLine(WIDTH/2+this.xSample*this.scaleFactor, this.cameraY, WIDTH/2+this.xSample*this.scaleFactor, this.cameraY+HEIGHT);

		// Numbers
		gc.setFill(Color.BLACK);
		gc.setFont(new Font("sans-serif", 15));
		gc.setTextAlign(TextAlignment.CENTER);
		double numStep = 40/this.scaleFactor;

		// Positive X
		for (double x = 0; x < (this.cameraX+WIDTH/2)/this.scaleFactor; x += numStep){
			gc.fillText(String.format("%.1f", x), WIDTH/2+x*this.scaleFactor, HEIGHT/2+17);
		}
		// Negative X
		for (double x = numStep; x < (WIDTH-(this.cameraX+WIDTH/2))/this.scaleFactor; x += numStep){
			gc.fillText(String.format("%.1f", -x), WIDTH/2-x*this.scaleFactor, HEIGHT/2+17);
		}
		// Positive Y
		for (double y = numStep; y < (HEIGHT-(this.cameraY+HEIGHT/2))/this.scaleFactor; y += numStep){
			gc.fillText(String.format("%.1f", y), WIDTH/2+17, HEIGHT/2-y*this.scaleFactor);
		}
		// Negative Y
		for (double y = numStep; y < (this.cameraY+HEIGHT/2)/this.scaleFactor; y += numStep){
			gc.fillText(String.format("%.1f", -y), WIDTH/2+17, HEIGHT/2+y*this.scaleFactor);
		}

		for (GraphFunction func : this.functions){
			List<Pair<Double, Double>> result = func.getResult().getValue();
			gc.setStroke(func.getColor());
			gc.setLineWidth(1.5);
			for (int i = 0; i < result.size(); i++){
				Pair<Double, Double> point = result.get(i);
				if (point.getValue() != null && !point.getValue().isNaN()){
					Pair<Double, Double> next = i == result.size()-1 ? null : result.get(i+1);
					if (next != null && next.getValue() != null && !next.getValue().isNaN() && Math.abs(next.getValue()) < Integer.MAX_VALUE){
						gc.strokeLine(WIDTH/2+point.getKey()*this.scaleFactor, HEIGHT/2-point.getValue()*this.scaleFactor, WIDTH/2+next.getKey()*this.scaleFactor, HEIGHT/2-next.getValue()*this.scaleFactor);
					}
				}
			}
		}
		gc.restore();
	}

	public static void main(String[] args){
		launch(args);
	}
}