package de.moritz.fastimageviewer.main;

import com.google.inject.Guice;
import com.google.inject.Injector;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class Main extends Application {

	private static Injector i;
	private FXMLLoader fxmlLoader;
	public static void main(String[] args) {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");

		i = Guice.createInjector(new DiModule(args.length > 0 ? args : null));
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		fxmlLoader = new FXMLLoader(getClass().getResource("/layout.fxml"));
		MainController controller = i.getInstance(MainController.class);
		fxmlLoader.setController(controller);
		primaryStage.setOnShown(event -> controller.onReady());
		Scene scene = new Scene(fxmlLoader.load());
		Screen screen = Screen.getPrimary();
		Rectangle2D bounds = screen.getVisualBounds();
		primaryStage.setX(0);
		primaryStage.setY(0);
		primaryStage.setWidth(bounds.getWidth());
		primaryStage.setHeight(bounds.getHeight());
		primaryStage.setScene(scene);
		primaryStage.show();

	}

}
