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

		this.functions.add(new GraphFunction(Color.BLUE, x -> Math.sqrt(5-x*x)));
		this.functions.add(new GraphFunction(Color.RED, x -> 2*x));
		this.functions.add(new GraphFunction(Color.GREEN, x -> Math.tan(x)));
		for (GraphFunction func : this.functions){
			func.buildInterval(-15, 15, 0.005);
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
		List<Pair<Double, Double>> result1 = f1.getResult().getValue();
		List<Pair<Double, Double>> result2 = f2.getResult().getValue();
		List<Double> output = new ArrayList<>();

		for (int i = 0; i < result1.size(); i++){ // TODO
			double delta = Math.abs(result1.get(i).getValue()-result2.get(i).getValue());
			if (delta < 0.01){
				final double solution = findPoint(f1, f2, result1.get(i).getKey(), f1.getResult().getStep(), 1);
				if (output.stream().filter(s -> Math.abs(s-solution) < 0.2).findAny().isEmpty()){
					output.add(solution);
				}
			}
		}

		return output;
	}

	private double findPoint(GraphFunction f1, GraphFunction f2, double value, double step, int depth){
		if (depth == 50) return (value+step/2+value-step/2)/2;
		
		double delta1 = Math.abs(f1.getDefinition().apply(value-step/2)-f2.getDefinition().apply(value-step/2));
		double delta2 = Math.abs(f1.getDefinition().apply(value+step/2)-f2.getDefinition().apply(value+step/2));
		if (delta1 < delta2){
			return findPoint(f1, f2, value-step/2, step/2, depth+1);
		} else if (delta1 > delta2){
			return findPoint(f1, f2, value+step/2, step/2, depth+1);
		} else {
			return value; // TODO
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

		gc.save();
		gc.translate(-this.cameraX, -this.cameraY);

		// Axes
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(3);
		gc.strokeLine(WIDTH/2, this.cameraY, WIDTH/2, this.cameraY+HEIGHT);
		gc.strokeLine(this.cameraX, HEIGHT/2, this.cameraX+WIDTH, HEIGHT/2);

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
				Pair<Double, Double> next = i == result.size()-1 ? null : result.get(i+1);
				if (next != null && Math.abs(next.getValue()) < Integer.MAX_VALUE){
					gc.strokeLine(WIDTH/2+point.getKey()*this.scaleFactor, HEIGHT/2-point.getValue()*this.scaleFactor, WIDTH/2+next.getKey()*this.scaleFactor, HEIGHT/2-next.getValue()*this.scaleFactor);
				}
			}
		}
		gc.restore();
	}

	public static void main(String[] args){
		launch(args);
	}
}