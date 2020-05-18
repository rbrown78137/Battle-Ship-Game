package src;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
public class Launcher extends Application {
	public static void main(String[] args) {
		 launch(args);
	}
	public void start(Stage primaryStage) {
		GridPane gridpane = new GridPane();
		Button host = new Button("Host");
		host.setOnAction(e->{
			startServer();
			startGameAsHost();
			primaryStage.close();
		});
		Button join = new Button("Join Online Game");
		Label label = new Label("  IP of Host:  ");
		TextField textfield = new TextField();
		join.setOnAction(e->{
			String hostName = textfield.getText();
			startGame(hostName);
			primaryStage.close();
		});
		gridpane.add(host,1,0);
		gridpane.add(join, 0, 1);
		gridpane.add(label,1,1);
		gridpane.add(textfield, 2, 1);
		Scene scene = new Scene(gridpane, 400, 400);
		primaryStage.setScene(scene);
		primaryStage.setTitle("BattleShip");
		primaryStage.show();
}
	public void startGameAsHost() {
		Platform.runLater(()->{
			Game game = new Game();
			game.hostName = "localhost";
			game.start(new Stage());
		});
	}
	public void startGame(String hostName) {
		Platform.runLater(()->{
			Game game = new Game();
			game.hostName = hostName;
			game.start(new Stage());
		});
	}
	public void startServer() {
		Platform.runLater(()->{
			new Server().start(new Stage());
		});
	}
}