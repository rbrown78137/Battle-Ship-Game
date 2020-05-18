package src;

import javafx.scene.Scene;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollPane;
import java.io.*;
import java.net.*;
import java.util.Date;

public class Server extends Application implements BattleShipConstants {
	private int sessionNo = 1;

	public void start(Stage primaryStage) {
		TextArea taLog = new TextArea();
		Scene scene = new Scene(new ScrollPane(taLog), 450, 200);
		primaryStage.setTitle("BattleShip Server");
		primaryStage.setScene(scene);
		primaryStage.show();
		new Thread(() -> {
			try {
				ServerSocket serverSocket = new ServerSocket(8000);
				Platform.runLater(() -> taLog.appendText(new Date() + ": Server started at socket 8000\n"));
				primaryStage.setOnCloseRequest(e -> {
					System.out.println("closing");
					try {
						serverSocket.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
				while (true) {
					Platform.runLater(() -> taLog
							.appendText(new Date() + ": Wait for players to join session " + sessionNo + "\n"));

					Socket player1 = serverSocket.accept();
					Platform.runLater(() -> {
						taLog.appendText(new Date() + ": Player 1 joined session " + sessionNo + "\n");
						taLog.appendText("Player 1's IP address: " + player1.getInetAddress().getHostAddress() + "\n");
					});
					new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);

					Socket player2 = serverSocket.accept();

					Platform.runLater(() -> {
						taLog.appendText(new Date() + ": Player 2 joined session " + sessionNo + '\n');
						taLog.appendText("Player 2's IP address" + player2.getInetAddress().getHostAddress() + '\n');
					});
					new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);

					Platform.runLater(
							() -> taLog.appendText(new Date() + ": Start a thread for session " + sessionNo++ + '\n'));
					new Thread(new HandleGame(player1, player2)).start();

				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		}).start();

	}

	class HandleGame implements Runnable, BattleShipConstants {
		private Socket player1;
		private Socket player2;

		private DataInputStream fromPlayer1;
		private DataOutputStream toPlayer1;
		private DataInputStream fromPlayer2;
		private DataOutputStream toPlayer2;

		private int[][] player1HMBoard = new int[10][10];
		private int[][] player2HMBoard = new int[10][10];
		private int[][] player1ShipLocation = new int[10][10];
		private int[][] player2ShipLocation = new int[10][10];

		private int currentPlayer = 1;

		public HandleGame(Socket player1, Socket player2) {
			this.player1 = player1;
			this.player2 = player2;
		}

		public void run() {
			try {
				 fromPlayer1 = new DataInputStream(player1.getInputStream());
				 toPlayer1 = new DataOutputStream(player1.getOutputStream());
				 fromPlayer2 = new DataInputStream(player2.getInputStream());
				 toPlayer2 = new DataOutputStream(player2.getOutputStream());
				for (int i = 0; i < 10; i++) {
					for (int k = 0; k < 10; k++) {
						player1ShipLocation[i][k] = fromPlayer1.readInt();
					}
				}
				for (int i = 0; i < 10; i++) {
					for (int k = 0; k < 10; k++) {
						player2ShipLocation[i][k] = fromPlayer2.readInt();
					}
				}
				System.out.println("Server: Game Starting");
				toPlayer1.writeInt(10);
				toPlayer2.writeInt(10);
				while (true) {
					if (currentPlayer == PLAYER1) {
						toPlayer1.writeInt(PLAYER1);
						toPlayer2.writeInt(PLAYER1);
						int row = fromPlayer1.readInt();
						int column = fromPlayer1.readInt();
						if (player2ShipLocation[row][column] != 0) {
							player1HMBoard[row][column] = HIT;
							toPlayer1.writeInt(HIT);
							toPlayer2.writeInt(HIT);
						} else {
							player1HMBoard[row][column] = MISS;
							toPlayer1.writeInt(MISS);
							toPlayer2.writeInt(MISS);
						}
						toPlayer1.writeInt(row);
						toPlayer1.writeInt(column);
						toPlayer2.writeInt(row);
						toPlayer2.writeInt(column);

					}
					else {
						toPlayer1.writeInt(PLAYER2);
						toPlayer2.writeInt(PLAYER2);
						int row = fromPlayer2.readInt();
						int column = fromPlayer2.readInt();
						if (player1ShipLocation[row][column] != 0) {
							player2HMBoard[row][column] = HIT;
							toPlayer1.writeInt(HIT);
							toPlayer2.writeInt(HIT);
						} else {
							player2HMBoard[row][column] = MISS;
							toPlayer1.writeInt(MISS);
							toPlayer2.writeInt(MISS);
						}
						toPlayer1.writeInt(row);
						toPlayer1.writeInt(column);
						toPlayer2.writeInt(row);
						toPlayer2.writeInt(column);
					}
					toPlayer1.writeInt(isWinner());
					toPlayer2.writeInt(isWinner());
					if(currentPlayer == PLAYER1) {
						currentPlayer = PLAYER2;
					}
					else if(currentPlayer == PLAYER2) {
						currentPlayer = PLAYER1;
					}
				}

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		public int isWinner() {
			boolean player1Win = true;
			boolean player2Win = true;
			for(int i = 0; i<10;i++) {
				for(int k = 0; k<10;k++) {
					if(player1ShipLocation[i][k] != 0) {
						if(player2HMBoard[i][k] != HIT) {
							player2Win = false;
						}
					}
					if(player2ShipLocation[i][k] != 0) {
						if(player1HMBoard[i][k] != HIT) {
							player1Win = false;
						}
					}
				}
			}
			if(player1Win) {
				return PLAYER1;
			}
			else if(player2Win) {
				return PLAYER2;
			}
			else {
				return 0;
			}
		}

	}

}