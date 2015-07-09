package de.moritz.fastimageviewer.main;

import com.google.inject.Guice;
import com.google.inject.Injector;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Main extends Application {

	private static Injector i;

	public static void main(String[] args) {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");

		i = Guice.createInjector(new DiModule(args.length > 0 ? args[0] : null));
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		Screen screen = Screen.getPrimary();
		Rectangle2D bounds = screen.getVisualBounds();
		primaryStage.setX(0);
		primaryStage.setY(0);
		primaryStage.setWidth(bounds.getWidth());
		primaryStage.setHeight(bounds.getHeight());
		Scene scene = new Scene(i.getInstance(Parent.class));
		primaryStage.setScene(scene);
		primaryStage.show();

	}

}
