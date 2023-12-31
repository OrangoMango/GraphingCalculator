package com.orangomango.graphcalc;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import javafx.util.Pair;
import javafx.util.Callback;

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
		GridPane pane = new GridPane();
		pane.setPadding(new Insets(5, 5, 5, 5));
		pane.setHgap(5);
		pane.setVgap(5);

		// Menu
		MenuBar menuBar = new MenuBar();
		Menu fileMenu = new Menu("File");
		Menu editMenu = new Menu("Edit");
		MenuItem editGraphs = new MenuItem("Edit graphs");
		editGraphs.setOnAction(e -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Edit");
			alert.setHeaderText("Edit graphs");
			GridPane content = new GridPane();
			content.setPrefWidth(350);
			content.setPrefHeight(150);
			alert.getDialogPane().setContent(content);
			content.setPadding(new Insets(5, 5, 5, 5));
			content.setHgap(5);
			content.setVgap(5);
			ListView<GraphFunction> list = new ListView<>();
			list.setCellFactory(param -> new ListCell<>(){
				@Override
				public void updateItem(GraphFunction func, boolean empty){
					super.updateItem(func, empty);
					if (empty){
						setText(null);
						setGraphic(null);
					} else if (func != null){
						setText(func.toString());
						setGraphic(new Rectangle(15, 15, func.getColor()));
					}
				}
			});
			for (GraphFunction f : this.functions){
				list.getItems().add(f);
			}
			content.add(list, 0, 0);
			Button add = new Button("Add");
			add.setOnAction(ev -> {
				Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
				dialog.setTitle("Add equation");
				GridPane gpane = new GridPane();
				gpane.setPadding(new Insets(5, 5, 5, 5));
				gpane.setHgap(5);
				gpane.setVgap(5);
				Label label = new Label("Equation: ");
				TextField field = new TextField();
				ColorPicker picker = new ColorPicker(Color.color(Math.random(), Math.random(), Math.random()));
				gpane.add(label, 0, 0);
				gpane.add(field, 1, 0);
				gpane.add(picker, 0, 1, 2, 1);
				field.requestFocus();
				dialog.getDialogPane().setContent(gpane);
				dialog.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
					synchronized (this){
						GraphFunction f = new GraphFunction(picker.getValue(), field.getText());
						GraphFunction.addFunction(this.functions, f);
						list.getItems().add(f);
					}
				});
			});
			Button remove = new Button("Remove");
			remove.setOnAction(ev -> {
				List<GraphFunction> selected = new ArrayList<>(list.getSelectionModel().getSelectedItems());
				synchronized (this){
					for (GraphFunction f : selected){
						GraphFunction.removeFunction(this.functions, f);
						list.getItems().remove(f);
					}
				}
			});
			content.add(new VBox(5, add, remove), 1, 0);
			alert.showAndWait();
		});
		MenuItem calculate = new MenuItem("Find intersection");
		calculate.setOnAction(e -> {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Calculate intersection");
			alert.setHeaderText("Select 2 equations");
			GridPane gpane = new GridPane();
			gpane.setPadding(new Insets(5, 5, 5, 5));
			gpane.setVgap(5);
			gpane.setHgap(5);
			ChoiceBox<GraphFunction> box = new ChoiceBox<>();
			for (GraphFunction f : this.functions){
				box.getItems().add(f);
			}
			ChoiceBox<GraphFunction> box2 = new ChoiceBox<>();
			for (GraphFunction f : this.functions){
				box2.getItems().add(f);
			}
			box.setMinWidth(150);
			box2.setMinWidth(150);
			if (this.functions.size() > 0) box.getSelectionModel().select(0);
			if (this.functions.size() > 0) box2.getSelectionModel().select(this.functions.size() < 2 ? 0 : 1);
			gpane.add(box, 0, 0);
			gpane.add(box2, 0, 1);
			alert.getDialogPane().setContent(gpane);
			alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
				// Calculate the result
				Alert info = new Alert(Alert.AlertType.INFORMATION);
				info.setTitle("Resul");
				info.setHeaderText("Intersections found:");
				List<Double> output = findIntersections(box.getSelectionModel().getSelectedItem(), box2.getSelectionModel().getSelectedItem());
				StringBuilder builder = new StringBuilder();
				output.stream().forEach(d -> builder.append(String.format("x = %.3f\n", d)));
				info.setContentText(builder.toString());
				info.showAndWait();
			});
		});

		menuBar.getMenus().addAll(fileMenu, editMenu);
		editMenu.getItems().addAll(editGraphs, calculate);
		pane.add(menuBar, 0, 0);

		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		pane.add(canvas, 0, 1);
		GraphicsContext gc = canvas.getGraphicsContext2D();

		canvas.setFocusTraversable(true);
		canvas.setOnKeyPressed(e -> this.keys.put(e.getCode(), true));
		canvas.setOnKeyReleased(e -> this.keys.put(e.getCode(), false));

		canvas.setOnScroll(e -> {
			if (e.getDeltaY() > 0){
				this.scaleFactor += 2;
			} else if (e.getDeltaY() < 0){
				this.scaleFactor -= 2;
			}

			this.scaleFactor = Math.min(120, Math.max(this.scaleFactor, 20));
		});

		//GraphFunction.addFunction(this.functions, new GraphFunction(Color.BLUE, "y = abs(x+6)"));
		GraphFunction.addFunction(this.functions, new GraphFunction(Color.RED, "5*y^2-6*x*y = -5*x^2+8"));

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

		Scene scene = new Scene(pane, WIDTH+20, HEIGHT+50);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();
	}

	// TODO: support quadratic equations
	private List<Double> findIntersections(GraphFunction f1, GraphFunction f2){
		List<Double> output = new ArrayList<>();

		for (double i = -10; i < 10; i += 0.001){
			double delta = Math.abs(f1.getDefinition().apply(i)-f2.getDefinition().apply(i));
			if (delta < 0.01){
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

		final int cameraSpeed = 8;
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

		synchronized (this){
			gc.setLineWidth(1.5);
			for (GraphFunction func : this.functions){
				gc.setStroke(func.getColor());
				for (Result rs : func.getResults()){
					List<Pair<Double, Double>> result = rs.getValue();
					for (int i = 0; i < result.size(); i++){
						Pair<Double, Double> point = result.get(i);
						if (point.getValue() != null && !point.getValue().isNaN()){
							Pair<Double, Double> next = i == result.size()-1 ? null : result.get(i+1);
							if (next != null && next.getValue() != null && !next.getValue().isNaN() && Math.abs(next.getValue()) < Integer.MAX_VALUE){
								drawLine(gc, point, next);
							}
						}
					}
				}

				// Connect the bounds of the results if it's a quadratic equation
				if (func.isQuadratic()){
					int firstN = -1;
					int lastN = -1;
					for (int i = 0; i < func.getResults().get(0).getValue().size(); i++){
						Double y1 = func.getResults().get(0).getValue().get(i).getValue();
						Double y2 = func.getResults().get(1).getValue().get(i).getValue();
						if (y1 != null && firstN == -1){
							firstN = i;
						}
						if (y1 == null && lastN == -1 && firstN != -1){
							lastN = i-1;
						}
					}
					drawLine(gc, func.getResults().get(0).getValue().get(firstN), func.getResults().get(1).getValue().get(firstN));
					drawLine(gc, func.getResults().get(0).getValue().get(lastN), func.getResults().get(1).getValue().get(lastN));
				}
			}
		}
		gc.restore();
	}

	private void drawLine(GraphicsContext gc, Pair<Double, Double> point, Pair<Double, Double> next){
		gc.strokeLine(WIDTH/2+point.getKey()*this.scaleFactor, HEIGHT/2-point.getValue()*this.scaleFactor, WIDTH/2+next.getKey()*this.scaleFactor, HEIGHT/2-next.getValue()*this.scaleFactor);
	}

	public static void main(String[] args){
		launch(args);
	}
}