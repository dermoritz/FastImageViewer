package de.moritz.fastimageviewer.main;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	private static Injector i;
	public static void main(String[] args) {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");

		i = Guice.createInjector(new DiModule(args.length > 0 ? args : null));
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/layout.fxml"));
		MainController controller = i.getInstance(MainController.class);
		fxmlLoader.setController(controller);
		Scene scene = new Scene(fxmlLoader.load());
		primaryStage.setScene(scene);
		primaryStage.setMaximized(true);
		primaryStage.show();
		controller.onReady();
	}

}
