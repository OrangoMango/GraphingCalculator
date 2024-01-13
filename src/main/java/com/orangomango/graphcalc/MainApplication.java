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

import com.orangomango.graphcalc.math.*;

public class MainApplication extends Application{
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int FPS = 40;
	private static final int MAX_INTERSECTION_COUNT = 15;

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

		// Edit menu
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

			list.getSelectionModel().select(0);
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
				GraphFunction selected = list.getSelectionModel().getSelectedItem();
				synchronized (this){
					GraphFunction.removeFunction(this.functions, selected);
					list.getItems().remove(selected);
				}
			});
			ColorPicker changeColor = new ColorPicker();
			if (this.functions.size() > 0) changeColor.setValue(this.functions.get(0).getColor());
			changeColor.setOnAction(ev -> list.getSelectionModel().getSelectedItem().setColor(changeColor.getValue()));
			list.getSelectionModel().selectedItemProperty().addListener((ob, oldV, newV) -> changeColor.setValue(newV.getColor()));
			content.add(new VBox(5, add, remove, changeColor), 1, 0);
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
				info.setTitle("Result");
				info.setHeaderText("Intersections found:");
				List<Double> output = findIntersections(box.getSelectionModel().getSelectedItem(), box2.getSelectionModel().getSelectedItem());
				StringBuilder builder = new StringBuilder();
				if (output == null){
					builder.append("The two equations intersect in at least "+MAX_INTERSECTION_COUNT+" points");
				} else if (output.size() == 0){
					builder.append("No intersection");
				} else {
					output.stream().forEach(d -> builder.append(String.format("x = %.3f\n", d)));
				}
				info.setContentText(builder.toString());
				info.showAndWait();
			});
		});

		// Transform menu
		Menu transformMenu = new Menu("Transform");
		MenuItem applyTransform = new MenuItem("Apply transformation");
		applyTransform.setOnAction(e -> {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Apply transformation");
			GridPane gpane = new GridPane();
			gpane.setPadding(new Insets(5, 5, 5, 5));
			gpane.setVgap(5);
			gpane.setHgap(5);
			ChoiceBox<GraphFunction> box = new ChoiceBox<>();
			for (GraphFunction f : this.functions){
				box.getItems().add(f);
			}
			box.getSelectionModel().select(0);
			ColorPicker picker = new ColorPicker(Color.color(Math.random(), Math.random(), Math.random()));
			Label xPrime = new Label("x' = ");
			Label yPrime = new Label("y' = ");
			TextField xEq = new TextField();
			TextField yEq = new TextField();
			gpane.add(box, 0, 0, 2, 1);
			gpane.add(picker, 0, 1, 2, 1);
			gpane.add(xPrime, 0, 2);
			gpane.add(yPrime, 0, 3);
			gpane.add(xEq, 1, 2);
			gpane.add(yEq, 1, 3);
			alert.getDialogPane().setContent(gpane);
			alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
				Transformation t = new Transformation("x'="+xEq.getText(), "y'="+yEq.getText());
				GraphFunction transformed = box.getSelectionModel().getSelectedItem().transform(picker.getValue(), t.getDefX(), t.getDefY());
				GraphFunction.addFunction(this.functions, transformed);
			});
		});

		menuBar.getMenus().addAll(fileMenu, editMenu, transformMenu);
		editMenu.getItems().addAll(editGraphs, calculate);
		transformMenu.getItems().add(applyTransform);
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

		/*GraphFunction.addFunction(this.functions, new GraphFunction(Color.GREEN, "x^2+y^2=9"));
		GraphFunction.addFunction(this.functions, new GraphFunction(Color.BLUE, "x^2/4+y^2=1"));
		GraphFunction.addFunction(this.functions, this.functions.get(1).transform(Color.RED, "x = x'*cos(PI/4)-y'*sin(PI/4)", "y = x'*sin(PI/4)+y'*cos(PI/4)"));
		GraphFunction.addFunction(this.functions, this.functions.get(0).transform(Color.CYAN, "x = x'-2", "y = y'-2"));
		GraphFunction.addFunction(this.functions, this.functions.get(2).transform(Color.ORANGE, "x = -x'", "y = y'"));*/

		AnimationTimer timer = new AnimationTimer(){
			@Override
			public void handle(long time){
				update(gc);
				MainApplication.this.frames++;
				stage.setTitle("Graphing calculator - FPS:"+MainApplication.this.fps); // Set FPS title
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

	private List<Double> findIntersections(GraphFunction f1, GraphFunction f2){
		List<Double> result = new ArrayList<>();

		/*for (double i = -10; i < 10; i += 0.001){
			List<Double> y1 = f1.solveForY(i);
			List<Double> y2 = f2.solveForY(i);

			for (int j = 0; j < y1.size(); j++){
				for (int k = 0; k < y2.size(); k++){
					double delta = Math.abs(y1.get(j)-y2.get(k));
					if (delta < 0.1){
						//System.out.format("Possible solution: %s\n", i);
						result.add(i);
					}
				}
			}
		}*/

		return result;
	}

	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#F0F0F0"));
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
					List<Pair<Double, Double>> result = rs.getValues();
					for (int i = 0; i < result.size(); i++){
						Pair<Double, Double> point = result.get(i);
						if (point.getValue() != null && !point.getValue().isNaN()){
							if (point.getValue().isInfinite()){
								// TODO: hardcoded -10 and 10
								drawLine(gc, new Pair<Double, Double>(point.getKey(), -10.0), new Pair<Double, Double>(point.getKey(), 10.0));
							} else {
								Pair<Double, Double> next = i == result.size()-1 ? null : result.get(i+1);
								if (next != null && next.getValue() != null && !next.getValue().isNaN() && Math.abs(next.getValue()) < Integer.MAX_VALUE){
									drawLine(gc, point, next);
								}
							}
						}
					}
				}

				// Connect the bounds of the results if it's a quadratic equation
				if (func.isQuadratic()){
					for (int i = 0; i < func.getResults().get(0).getValues().size(); i++){
						Double y1 = func.getResults().get(0).getValues().get(i).getValue();
						Double y2 = func.getResults().get(1).getValues().get(i).getValue();
						if (y1 != null && y2 != null && Math.abs(y1-y2) < 1){ // TODO: Check function's trend instead of < 1
							if (i < func.getResults().get(0).getValues().size()-1){
								Double ny1 = func.getResults().get(0).getValues().get(i+1).getValue();
								Double ny2 = func.getResults().get(1).getValues().get(i+1).getValue();
								if (ny1 == null && ny2 == null){
									drawLine(gc, func.getResults().get(0).getValues().get(i), func.getResults().get(1).getValues().get(i));
								}
							}
							if (i > 0){
								Double py1 = func.getResults().get(0).getValues().get(i-1).getValue();
								Double py2 = func.getResults().get(1).getValues().get(i-1).getValue();
								if (py1 == null && py2 == null){
									drawLine(gc, func.getResults().get(0).getValues().get(i), func.getResults().get(1).getValues().get(i));
								}
							}
						}
					}
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