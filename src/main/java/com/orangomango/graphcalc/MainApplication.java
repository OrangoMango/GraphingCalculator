package com.orangomango.graphcalc;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.Pair;
import javafx.util.Callback;

import java.util.*;
import java.io.*;

import com.orangomango.graphcalc.math.*;

public class MainApplication extends Application{
	public static final int WIDTH = 1000;
	public static final int HEIGHT = 800;
	private static final int FPS = 40;
	private static final int MAX_INTERSECTION_COUNT = 100;
	public static final Font FONT = new Font("sans-serif", 15);

	private Map<KeyCode, Boolean> keys = new HashMap<>();
	private int frames, fps;
	private List<GraphElement> elements = new ArrayList<>();
	private double cameraX, cameraY;
	private double scaleFactor = 40;
	private double oldMouseX, oldMouseY;
	private boolean movingScene;
	private Point2D mouseCoord = Point2D.ZERO;
	private Pair<GraphFunction, Pair<Double, Double>> hoveringPoint;
	private double leftPos, rightPos, topPos, bottomPos;
	private Map<String, Double> parameters = new HashMap<>();

	@Override
	public void start(Stage stage){
		GridPane pane = new GridPane();
		pane.setPadding(new Insets(5, 5, 5, 5));
		pane.setHgap(5);
		pane.setVgap(5);

		// Menu
		MenuBar menuBar = new MenuBar();
		Menu fileMenu = new Menu("File");
		MenuItem save = new MenuItem("Save");
		save.setOnAction(e -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Save file");
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GraphCalc files", "*.gcalc"));
			File file = chooser.showSaveDialog(stage);
			if (file != null){
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(file));
					for (GraphElement element : this.elements){
						writer.write(String.format("%s:%s %s %s %s\n", element.getIdentifier().getId(), element.toString(), element.getColor().getRed(), element.getColor().getGreen(), element.getColor().getBlue()));
					}
					writer.close();
				} catch (IOException ex){
					ex.printStackTrace();
				}

				Alert info = new Alert(Alert.AlertType.INFORMATION);
				info.setTitle("File");
				info.setHeaderText("File saved successfully");
				info.showAndWait();
			}
		});
		MenuItem load = new MenuItem("Load");
		load.setOnAction(e -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Load file");
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GraphCalc files", "*.gcalc"));
			File file = chooser.showOpenDialog(stage);
			if (file != null){
				try {
					BufferedReader reader = new BufferedReader(new FileReader(file));
					String line;
					while ((line = reader.readLine()) != null){
						if (!line.isBlank()){
							String[] pieces = line.split(" ");
							Color color = Color.color(Double.parseDouble(pieces[1]), Double.parseDouble(pieces[2]), Double.parseDouble(pieces[3]));
							Identifier id = Identifier.parse(pieces[0].split(":")[0]);
							switch (id){
								case GRAPH_FUNCTION:
									GraphFunction func = new GraphFunction(color, pieces[0].split(":")[1]);
									GraphFunction.addFunction(this.elements, func, this.leftPos, this.rightPos, this.parameters);
									break;
								case GRAPH_POINT:
									this.elements.add(new GraphPoint(color, pieces[0].split(":")[1]));
									break;
								case GRAPH_LINE:
									this.elements.add(new GraphLine(color, pieces[0].split(":")[1], this.elements));
									break;
							}
						}
					}
					reader.close();
				} catch (IOException ex){
					ex.printStackTrace();
				}

				Alert info = new Alert(Alert.AlertType.INFORMATION);
				info.setTitle("File");
				info.setHeaderText("File loaded successfully");
				info.showAndWait();
			}
		});

		// Edit menu
		Menu editMenu = new Menu("Edit");
		MenuItem editGraphs = new MenuItem("Edit graphs");
		editGraphs.setOnAction(e -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Edit");
			alert.setHeaderText("Edit graphs");
			GridPane content = new GridPane();
			content.setPrefWidth(600);
			content.setPrefHeight(350);
			alert.getDialogPane().setContent(content);
			content.setPadding(new Insets(5, 5, 5, 5));
			content.setHgap(5);
			content.setVgap(5);
			ListView<GraphElement> list = new ListView<>();
			list.setMinWidth(400);
			list.setMinHeight(300);
			list.setCellFactory(param -> new ListCell<>(){
				@Override
				public void updateItem(GraphElement element, boolean empty){
					super.updateItem(element, empty);
					if (empty){
						setText(null);
						setGraphic(null);
					} else if (element != null){
						setText(element.toString());
						setGraphic(new Rectangle(15, 15, element.getColor()));
					}
				}
			});
			list.getItems().addAll(this.elements);
			list.getSelectionModel().select(0);
			content.add(list, 0, 0);
			Button add = new Button("Add");
			add.setOnAction(ev -> {
				Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
				dialog.setTitle("Add equation");
				dialog.setHeaderText("Add equation");
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
				dialog.setOnShown(eve -> Platform.runLater(field::requestFocus));
				dialog.getDialogPane().setContent(gpane);
				dialog.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
					try {
						String text = field.getText();
						synchronized (this){
							if (text.contains("=")){
								GraphFunction f = new GraphFunction(picker.getValue(), text);
								GraphFunction.addFunction(this.elements, f, this.leftPos, this.rightPos, this.parameters);
								list.getItems().add(f);
							} else {
								// TODO: Make more generic (support more types)
								if (text.startsWith("LINE")){
									GraphLine l = new GraphLine(picker.getValue(), text, this.elements);
									this.elements.add(l);
									list.getItems().add(l);
								} else {
									GraphPoint p = new GraphPoint(picker.getValue(), text);
									this.elements.add(p);
									list.getItems().add(p);
								}
							}
						}
					} catch (Exception ex){
						displayError(ex);
					}
				});
			});
			Button remove = new Button("Remove");
			remove.setOnAction(ev -> {
				GraphElement selected = list.getSelectionModel().getSelectedItem();
				if (selected != null){
					synchronized (this){
						this.elements.remove(selected);
						list.getItems().remove(selected);
					}
				}
			});
			Button edit = new Button("Edit");
			edit.setOnAction(ev -> {
				GraphElement selected = list.getSelectionModel().getSelectedItem();
				if (selected != null){
					try {
						TextInputDialog input = new TextInputDialog(selected.toString());
						input.setTitle("Edit");
						input.setHeaderText("Edit object: "+selected.getClass().getSimpleName());
						input.showAndWait().ifPresent(v -> {
							selected.edit(v, this.parameters);
							list.refresh();
						});
					} catch (Exception ex){
						displayError(ex);
					}
				}
			});
			ColorPicker changeColor = new ColorPicker();
			if (this.elements.size() > 0) changeColor.setValue(this.elements.get(0).getColor());
			changeColor.setOnAction(ev -> {
				GraphElement selected = list.getSelectionModel().getSelectedItem();
				if (selected != null){
					selected.setColor(changeColor.getValue());
					list.refresh();
				}
			});
			list.getSelectionModel().selectedItemProperty().addListener((ob, oldV, newV) -> {
				if (newV != null) changeColor.setValue(newV.getColor());
			});
			content.add(new VBox(5, add, remove, edit, changeColor), 1, 0);
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
			box.getItems().addAll(this.elements.stream().filter(gel -> gel instanceof GraphFunction).map(gel -> (GraphFunction)gel).toList());
			ChoiceBox<GraphFunction> box2 = new ChoiceBox<>();
			box2.getItems().addAll(this.elements.stream().filter(gel -> gel instanceof GraphFunction).map(gel -> (GraphFunction)gel).toList());
			box.setMinWidth(150);
			box2.setMinWidth(150);
			box.getSelectionModel().select(0);
			box2.getSelectionModel().select(box.getItems().size() < 2 ? 0 : 1);
			Rectangle rec1 = new Rectangle(25, 25, box.getItems().size() > 0 ? box.getItems().get(0).getColor() : Color.WHITE);
			Rectangle rec2 = new Rectangle(25, 25, box2.getSelectionModel().getSelectedItem() != null ? box2.getSelectionModel().getSelectedItem().getColor() : Color.WHITE);
			box.getSelectionModel().selectedItemProperty().addListener((ob, oldV, newV) -> rec1.setFill(newV.getColor()));
			box2.getSelectionModel().selectedItemProperty().addListener((ob, oldV, newV) -> rec2.setFill(newV.getColor()));
			gpane.add(box, 0, 0);
			gpane.add(box2, 0, 1);
			gpane.add(rec1, 1, 0);
			gpane.add(rec2, 1, 1);
			alert.getDialogPane().setContent(gpane);
			alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
				// Calculate the result
				try {
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
				} catch (Exception ex){
					displayError(ex);
				}
			});
		});

		// Transform menu
		Menu transformMenu = new Menu("Transform");
		MenuItem applyTransform = new MenuItem("Apply transformation");
		applyTransform.setOnAction(e -> {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Apply transformation");
			alert.setHeaderText("Transform");
			GridPane gpane = new GridPane();
			gpane.setPadding(new Insets(5, 5, 5, 5));
			gpane.setVgap(5);
			gpane.setHgap(5);
			ChoiceBox<Transformable> box = new ChoiceBox<>();
			box.getItems().addAll(this.elements.stream().filter(tr -> tr instanceof Transformable).map(tr -> (Transformable)tr).toList());
			box.getSelectionModel().select(0);
			Rectangle rec = new Rectangle(25, 25, box.getItems().size() > 0 ? ((GraphElement)box.getItems().get(0)).getColor() : Color.WHITE);
			box.getSelectionModel().selectedItemProperty().addListener((ob, oldV, newV) -> rec.setFill(((GraphElement)newV).getColor()));
			ColorPicker picker = new ColorPicker(Color.color(Math.random(), Math.random(), Math.random()));
			Label xPrime = new Label("x' = ");
			Label yPrime = new Label("y' = ");
			TextField xEq = new TextField();
			TextField yEq = new TextField();
			xEq.setPromptText("x+5");
			yEq.setPromptText("2*y-1");
			gpane.add(box, 0, 0, 2, 1);
			gpane.add(rec, 2, 0);
			gpane.add(picker, 0, 1, 2, 1);
			gpane.add(xPrime, 0, 2);
			gpane.add(yPrime, 0, 3);
			gpane.add(xEq, 1, 2);
			gpane.add(yEq, 1, 3);
			alert.getDialogPane().setContent(gpane);
			alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
				try {
					synchronized (this){
						Transformable selected = box.getSelectionModel().getSelectedItem();
						if (selected instanceof GraphFunction){
							Transformation t = new Transformation("x'="+xEq.getText(), "y'="+yEq.getText());
							GraphElement transformed = selected.transform(picker.getValue(), t.getDefX(), t.getDefY());
							GraphFunction.addFunction(this.elements, (GraphFunction)transformed, this.leftPos, this.rightPos, this.parameters);
						} else {
							GraphElement transformed = selected.transform(picker.getValue(), xEq.getText(), yEq.getText());
							this.elements.add(transformed);
						}
					}
				} catch (Exception ex){
					displayError(ex);
				}
			});
		});

		menuBar.getMenus().addAll(fileMenu, editMenu, transformMenu);
		fileMenu.getItems().addAll(save, load);
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
			// Update the results
			for (GraphFunction f : this.elements.stream().filter(gel -> gel instanceof GraphFunction).map(gel -> (GraphFunction)gel).toList()){
				f.expand(this.leftPos, this.rightPos, this.parameters);
			}
		});

		canvas.setOnMouseMoved(e -> {
			double x = (e.getX()-WIDTH/2+this.cameraX)/this.scaleFactor;
			double y = -(e.getY()-HEIGHT/2+this.cameraY)/this.scaleFactor;
			this.mouseCoord = new Point2D(x, y);
			Pair<GraphFunction, Pair<Double, Double>> found = null;
			fLoop:
			for (GraphFunction f : this.elements.stream().filter(gel -> gel instanceof GraphFunction).map(gel -> (GraphFunction)gel).toList()){
				for (Result r : f.getResults()){
					for (Pair<Double, Double> pair : r.getValues()){
						if (pair.getValue() != null && Math.abs(pair.getKey()-x) < 0.1 && (pair.getValue().isInfinite() || Math.abs(pair.getValue()-y) < 0.1)){
							found = new Pair<>(f, pair.getValue().isInfinite() ? new Pair<>(pair.getKey(), y) : pair);
							break fLoop;
						}
					}
				}
			}
			this.hoveringPoint = found;
		});

		canvas.setOnMousePressed(e -> {
			this.oldMouseX = e.getX();
			this.oldMouseY = e.getY();
			if (e.getButton() == MouseButton.PRIMARY){
				if (this.hoveringPoint != null){
					// TODO
					System.out.println(this.hoveringPoint.getKey());
				}
			}
		});

		canvas.setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				this.cameraX += this.oldMouseX-e.getX();
				this.cameraY += this.oldMouseY-e.getY();
				this.oldMouseX = e.getX();
				this.oldMouseY = e.getY();
				this.movingScene = true;

				// Update the results
				for (GraphFunction f : this.elements.stream().filter(gel -> gel instanceof GraphFunction).map(gel -> (GraphFunction)gel).toList()){
					f.expand(this.leftPos, this.rightPos, this.parameters);
				}
			}
		});

		canvas.setOnMouseReleased(e -> {
			this.movingScene = false;
		});

		Scene scene = new Scene(pane, WIDTH+20, HEIGHT+50);
		AnimationTimer timer = new AnimationTimer(){
			@Override
			public void handle(long time){
				update(gc);
				MainApplication.this.frames++;
				stage.setTitle("Graphing calculator - FPS:"+MainApplication.this.fps); // Set FPS title
				if (MainApplication.this.movingScene){
					scene.setCursor(Cursor.MOVE);
				} else {
					scene.setCursor(Cursor.DEFAULT);
				}
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

		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();
	}

	private List<Double> findIntersections(GraphFunction f1, GraphFunction f2){
		List<Double> result = new ArrayList<>();
		Expression eq1R = (Expression)f1.getEquation().getLeftSide().copy(null);
		Expression eq1L = (Expression)f1.getEquation().getRightSide().copy(null);
		Equation.prepareForSolving(eq1R, eq1L, "y", this.parameters);
		Expression eq2R = (Expression)f2.getEquation().getLeftSide().copy(null);
		Expression eq2L = (Expression)f2.getEquation().getRightSide().copy(null);
		Equation.prepareForSolving(eq2R, eq2L, "y", this.parameters);

		for (double i = f1.getResults().get(0).getFrom(); i < f1.getResults().get(0).getTo(); i += 0.001){
			Map<String, Double> params = new HashMap<>(this.parameters);
			params.put("x", i);
			List<Double> y1 = Equation.solve(eq1R, eq1L, "y", params);
			List<Double> y2 = Equation.solve(eq2R, eq2L, "y", params);

			for (int j = 0; j < y1.size(); j++){
				for (int k = 0; k < y2.size(); k++){
					double delta = Math.abs(y1.get(j)-y2.get(k));
					if (delta < 0.1 || Double.isInfinite(y1.get(j)) || Double.isInfinite(y2.get(k))){
						//System.out.println("sol: "+i);
						result.add(i);
					}
				}
			}
		}

		List<List<Double>> intervals = new ArrayList<>();
		List<Double> current = new ArrayList<>();
		Double lastStep = Double.NaN;
		for (int i = 1; i < result.size(); i++){
			double prev = result.get(i-1);
			double value = result.get(i);
			current.add(value);
			if (!lastStep.isNaN()){
				double diff = value-prev-lastStep;
				if (diff > 0.01 || i == result.size()-1){
					intervals.add(current);
					current = new ArrayList<>();
				}
			}
			lastStep = value-prev;
		}

		if (result.size() == 1){
			return result;
		} else if (intervals.size() == 1 && intervals.get(0).size() > MAX_INTERSECTION_COUNT){
			System.out.println(intervals);
			System.out.println(intervals.stream().map(l -> l.get(l.size()/2)).toList());
			return null;
		}

		return intervals.stream().map(l -> l.get(l.size()/2)).toList();
	}

	private static void displayError(Exception ex){
		Alert err = new Alert(Alert.AlertType.ERROR);
		err.setTitle("Error");
		err.setHeaderText("An error occured");
		err.setContentText(ex.getMessage());
		Label label = new Label("Stacktrace:");
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		TextArea area = new TextArea(sw.toString());
		area.setMinWidth(600);
		area.setMinHeight(200);
		area.setEditable(false);
		err.getDialogPane().setExpandableContent(new VBox(5, label, area));
		err.showAndWait();
	}

	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#D6D6D6"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		if (this.keys.getOrDefault(KeyCode.ESCAPE, false)){
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
		gc.setFont(FONT);
		gc.setTextAlign(TextAlignment.CENTER);
		double numStep = 40/this.scaleFactor;

		this.leftPos = -(WIDTH-(this.cameraX+WIDTH/2))/this.scaleFactor;
		this.rightPos =  (this.cameraX+WIDTH/2)/this.scaleFactor;
		this.topPos = (HEIGHT-(this.cameraY+HEIGHT/2))/this.scaleFactor;
		this.bottomPos = -(this.cameraY+HEIGHT/2)/this.scaleFactor;

		for (double x = 0; x < this.rightPos; x += numStep){
			gc.fillText(String.format("%.1f", x), WIDTH/2+x*this.scaleFactor, HEIGHT/2+17);
		}
		for (double x = numStep; x < -this.leftPos; x += numStep){
			gc.fillText(String.format("%.1f", -x), WIDTH/2-x*this.scaleFactor, HEIGHT/2+17);
		}
		for (double y = numStep; y < this.topPos; y += numStep){
			gc.fillText(String.format("%.1f", y), WIDTH/2+17, HEIGHT/2-y*this.scaleFactor);
		}
		for (double y = numStep; y < -this.bottomPos; y += numStep){
			gc.fillText(String.format("%.1f", -y), WIDTH/2+17, HEIGHT/2+y*this.scaleFactor);
		}

		synchronized (this){
			gc.setLineWidth(1.5);
			for (GraphElement element : this.elements){
				if (!(element instanceof GraphPoint)){
					element.render(gc, this.topPos, this.bottomPos, this.scaleFactor);
				}
			}

			// Points are rendered later
			for (GraphElement element : this.elements){
				if (element instanceof GraphPoint){
					element.render(gc, this.topPos, this.bottomPos, this.scaleFactor);
				}
			}
		}

		if (this.hoveringPoint != null){
			gc.setFill(this.hoveringPoint.getKey().getColor());
			gc.fillOval(WIDTH/2+this.hoveringPoint.getValue().getKey()*this.scaleFactor-5, HEIGHT/2-this.hoveringPoint.getValue().getValue()*this.scaleFactor-5, 10, 10);
		}

		gc.restore();

		// User information
		gc.setFill(Color.BLACK);
		gc.setFont(FONT);
		gc.setTextAlign(TextAlignment.LEFT);
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("Mouse at: %.2f %.2f\n", this.mouseCoord.getX(), this.mouseCoord.getY()));
		builder.append(String.format("NESW: %.2f %.2f %.2f %.2f", this.topPos, this.rightPos, this.bottomPos, this.leftPos));
		if (this.hoveringPoint != null){
			builder.append(String.format("\nHovering at [%s], x=%.2f y=%.2f", this.hoveringPoint.getKey().getEquation(), this.hoveringPoint.getValue().getKey(), this.hoveringPoint.getValue().getValue()));
		}
		gc.fillText(builder.toString(), 20, 30);
	}

	public static void main(String[] args){
		launch(args);

		/*Equation eq = new Equation("y=5+2+6*x+2*x*3");
		eq.getLeftSide().calculate(null);
		System.out.println(eq);
		System.exit(0);*/
	}
}