package src;
import java.io.*;
import java.net.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Line;
import javafx.scene.control.Button;
import javafx.scene.shape.Ellipse;
import javafx.scene.paint.Color;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.ArrayList;
import javafx.scene.shape.Circle;

public class Game extends Application implements BattleShipConstants {
	public String hostName;
	private int player;
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	private int[][] shipLocation = new int[10][10]; // [row][column]
	private boolean[] placedShip = { false, false, false, false, false };
	// private String host;
	private ReentrantLock lock = new ReentrantLock();
	private Condition canStart = lock.newCondition();
	private Label status = new Label();
	private ArrayList<BattleShip> ships = new ArrayList<>();
	private boolean readyToSend = false;
	private int sendingRow;
	private int sendingColumn;
	private GameBoard playerBoard;
	private GameBoard enemyBoard;
	Circle posMarker = new Circle(40);
	private boolean markerPlaced = false;
	Pane display;

	public void start(Stage primaryStage) {
		System.out.println(hostName);
		try {
			Socket socket = new Socket(hostName, 8000);
			fromServer = new DataInputStream(socket.getInputStream());
			toServer = new DataOutputStream(socket.getOutputStream());
			primaryStage.setOnCloseRequest(e -> {
				try {
					socket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		primaryStage.setTitle("Battle Ship");
		primaryStage.show();
		new Thread(() -> {
			try {
				player = fromServer.readInt();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				lock.lock();
				canStart.await();
				System.out.println("Game: game starting");
				createGameBoard(primaryStage);
				try {
					for (int i = 0; i < 10; i++) {
						for (int k = 0; k < 10; k++) {
							toServer.writeInt(shipLocation[i][k]);
						}
					}
					fromServer.readInt();

					Platform.runLater(() -> {
						status.setText("Server Ready");
					});
					while (true) {
						int currentPlayer = fromServer.readInt();
						if (currentPlayer == player) {
							Platform.runLater(() -> {
								status.setText("Your Turn");
							});
							readyToSend = false;
							if (currentPlayer != player) {
								enemyBoard.getChildren().remove(posMarker);
								markerPlaced = false;
							}
							while (!readyToSend) {
								Thread.sleep(1000);
							}
							toServer.writeInt(sendingRow);
							toServer.writeInt(sendingColumn);
						} else {
							Platform.runLater(() -> {
								status.setText("Enemy's Turn");
							});
						}
						int playStatus = fromServer.readInt();
						int row = fromServer.readInt();
						int column = fromServer.readInt();
						if (currentPlayer == player) {
							markEnemy(playStatus, row, column);
						} else {
							markFriendly(playStatus, row, column);
						}
						int winner = fromServer.readInt();

						if (winner == PLAYER1) {
							Platform.runLater(() -> {
								Label win = new Label("Player 1 Wins!");
								win.setTextFill(new Color(1,0,0,1));
								win.setScaleX(4);
								win.setScaleY(4);
								win.setLayoutX(800);
								win.setLayoutY(450);
								display.getChildren().add(win);
							});

						}
						if (winner == PLAYER2) {
							Platform.runLater(() -> {
								Label win = new Label("Player 2 Wins!");
								win.setTextFill(new Color(1,0,0,1));
								win.setScaleX(4);
								win.setScaleY(4);
								win.setLayoutX(800);
								win.setLayoutY(450);
								display.getChildren().add(win);
							});

						}
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}).start();

		createStartBoard(primaryStage);

	}

	public void markFriendly(int status, int row, int column) {
		Platform.runLater(() -> {
			playerBoard.getChildren().add(new Marker(status, row, column));
		});
	}

	public void markEnemy(int status, int row, int column) {
		Platform.runLater(() -> {
			enemyBoard.getChildren().add(new Marker(status, row, column));
		});
	}

	private void createGameBoard(Stage primaryStage) {
		Platform.runLater(() -> {
			display = new Pane();
			ImageView water =  new ImageView(new Image(getClass().getResourceAsStream("/res/water.png")));
			water.setLayoutX(50);
			water.setLayoutY(100);
			display.getChildren().add(water);
			display.getChildren().add(status);
			status.setLayoutX(850);
			status.setLayoutY(50);
			playerBoard = new GameBoard(true);
			playerBoard.setLayoutX(50);
			playerBoard.setLayoutY(100);
			enemyBoard = new GameBoard(false);
			enemyBoard.setLayoutX(950);
			enemyBoard.setLayoutY(100);
			ImageView water2 =  new ImageView(new Image(getClass().getResourceAsStream("/res/water.png")));
			water2.setLayoutX(950);
			water2.setLayoutY(100);
			display.getChildren().add(water2);
			Label label = new Label("Your Board");
			label.setLayoutX(450);
			label.setLayoutY(50);
			Label label2 = new Label("Enemy Board");
			label2.setLayoutX(1350);
			label2.setLayoutY(50);
			Button confirmShot = new Button("Confirm Shot");
			confirmShot.setLayoutX(1600);
			confirmShot.setLayoutY(50);
			confirmShot.setOnAction(e -> {
				readyToSend = true;
			});
			display.getChildren().addAll(playerBoard, enemyBoard, label, label2, confirmShot);
			status.setText("Waiting for Server");
			Scene scene = new Scene(display, 1800, 900);
			primaryStage.setScene(scene);
		});
	}

	private void createStartBoard(Stage primaryStage) {
		Platform.runLater(() -> {
			Pane display = new Pane();
			ImageView water =  new ImageView(new Image(getClass().getResourceAsStream("/res/water.png")));
			water.setLayoutX(50);
			water.setLayoutY(100);
			display.getChildren().add(water);
			Label turnIndicator = new Label("Place Your Ships");
			turnIndicator.setLayoutX(200);
			turnIndicator.setLayoutY(50);
			display.getChildren().add(turnIndicator);
			StartBoard startBoard = new StartBoard();
			startBoard.setLayoutX(50);
			startBoard.setLayoutY(100);
			display.getChildren().add(startBoard);
			primaryStage.setScene(new Scene(display, 900, 900));
			startBoard.requestFocus();
			Button placedShips = new Button("Start Game");
			display.getChildren().add(placedShips);
			placedShips.setLayoutX(600);
			placedShips.setLayoutY(50);
			placedShips.setOnAction(e -> {
				if (placedShip[0] && placedShip[1] && placedShip[2] && placedShip[3] && placedShip[4]) {
					lock.lock();
					System.out.println("signaling");
					canStart.signal();
					lock.unlock();
				}
			});
		});

	}

	class GameBoard extends Pane {
		public GameBoard(boolean yourBoard) {
			super();
			for (int i = 0; i <= 10; i++) {
				Line line1 = new Line(80 * i, 0, 80 * i, 800);
				line1.setStrokeWidth(3);
				Line line2 = new Line(0, 80 * i, 800, 80 * i);
				line2.setStrokeWidth(3);
				getChildren().addAll(line1, line2);
			}
			if (yourBoard) {
				for (int i = 0; i < ships.size(); i++) {
					int row = ships.get(i).row;
					int column = ships.get(i).column;
					getChildren().add(ships.get(i));
					ships.get(i).setLayoutY(row * 80 + 40);
					ships.get(i).setLayoutX(column * 80 + 40);
					if ((ships.get(i).length == 2 || ships.get(i).length == 4) && !ships.get(i).rotate) {
						ships.get(i).setLayoutY(row * 80);
					}
					if ((ships.get(i).length == 2 || ships.get(i).length == 4) && ships.get(i).rotate) {
						ships.get(i).setLayoutX(column * 80);
					}
				}
			} else {
				posMarker.setFill(new Color(1, 1, 0, 0.75));
				setOnMouseClicked(e -> {
					if (!markerPlaced) {
						getChildren().add(posMarker);
						markerPlaced = true;
					}
					int row = (int) (e.getY() / 80);
					int column = (int) (e.getX() / 80);
					sendingRow = row;
					sendingColumn = column;
					posMarker.setCenterX(column * 80 + 40);
					posMarker.setCenterY(row * 80 + 40);
				});

			}
		}
	}

	class StartBoard extends Pane {
		private BattleShip currentPiece;
		private double mouseX;
		private double mouseY;

		public StartBoard() {
			super();
			for (int i = 0; i <= 10; i++) {
				Line line1 = new Line(80 * i, 0, 80 * i, 800);
				line1.setStrokeWidth(3);
				Line line2 = new Line(0, 80 * i, 800, 80 * i);
				line2.setStrokeWidth(3);
				getChildren().addAll(line1, line2);
			}
			setOnMouseClicked(e -> {
				handleClick(e.getX(), e.getY());
			});
			setOnKeyPressed(e -> {
				System.out.println(e.getText());
				handleCircleCreation(e.getText());
			});
			setOnMouseMoved(e -> {
				mouseX = e.getX();
				mouseY = e.getY();
				if (currentPiece != null) {
					currentPiece.setLayoutX(mouseX);
					currentPiece.setLayoutY(mouseY);
				}
			});
		}

		private void handleClick(double x, double y) {
			requestFocus();
			System.out.println("X: " + x);
			System.out.println("Y: " + y);
			int row = (int)(y/80);
			int column = (int)(x/80);
			System.out.println("Row "+ row);
			System.out.println("Column "+ column);
			if (currentPiece != null) {
				if(shipLocation[row][column] == 0) {
					placePieceOnBoard(y, x);
					currentPiece = null;
				}
			}
				else{
					for(BattleShip ship: ships) {
						if(ship.type == shipLocation[row][column]) {
							currentPiece = ship;
							shipLocation[ship.row][ship.column] = 0;
							if(!ship.rotate) {
								switch(ship.length) {
								case 2:
									shipLocation[ship.row-1][ship.column] = 0;
								
									break;
								case 3:
									shipLocation[ship.row-1][ship.column] = 0;
									shipLocation[ship.row+1][ship.column] = 0;
									break;
								case 4:
									shipLocation[ship.row-1][ship.column] = 0;
									shipLocation[ship.row+1][ship.column] = 0;
									shipLocation[ship.row-2][ship.column] = 0;
									break;
								case 5:
									shipLocation[ship.row-1][ship.column] = 0;
									shipLocation[ship.row+1][ship.column] = 0;
									shipLocation[ship.row-2][ship.column] = 0;
									shipLocation[ship.row+2][ship.column] = 0;
									break;
								}
							}
							else {
								switch(ship.length) {
								case 2:
									shipLocation[ship.row][ship.column-1] = 0;
									break;
								case 3:
									shipLocation[ship.row][ship.column-1] = 0;
									shipLocation[ship.row][ship.column+1] = 0;
									break;
								case 4:
									shipLocation[ship.row][ship.column-1] = 0;
									shipLocation[ship.row][ship.column+1] = 0;
									shipLocation[ship.row][ship.column-2] = 0;
									
									break;
								case 5:
									shipLocation[ship.row][ship.column-1] = 0;
									shipLocation[ship.row][ship.column+1] = 0;
									shipLocation[ship.row][ship.column-2] = 0;
									shipLocation[ship.row][ship.column+2] = 0;
									break;
								}
							}
							printShipBoard();
						}
					}
				}
			
		}

		private void handleCircleCreation(String code) {
			System.out.println(code);
			if (Character.toLowerCase(code.charAt(0)) == 'r') {
				if (currentPiece.getRotate() == 0) {
					currentPiece.setRotate(90);
					currentPiece.rotate = true;
				} else if (currentPiece.getRotate() == 90) {
					currentPiece.setRotate(0);
					currentPiece.rotate = false;
				}
			} else if (code.charAt(0) == '1' && currentPiece == null && placedShip[0] == false) {
				BattleShip battleship = new BattleShip(2, 1);
				currentPiece = battleship;
				getChildren().add(battleship);
			} else if (code.charAt(0) == '2' && currentPiece == null && placedShip[1] == false) {
				BattleShip battleship = new BattleShip(3, 2);
				currentPiece = battleship;
				getChildren().add(battleship);
			} else if (code.charAt(0) == '3' && currentPiece == null && placedShip[2] == false) {
				BattleShip battleship = new BattleShip(3, 3);
				currentPiece = battleship;
				getChildren().add(battleship);
			} else if (code.charAt(0) == '4' && currentPiece == null && placedShip[3] == false) {
				BattleShip battleship = new BattleShip(4, 4);
				currentPiece = battleship;
				getChildren().add(battleship);
			} else if (code.charAt(0) == '5' && currentPiece == null && placedShip[4] == false) {
				BattleShip battleship = new BattleShip(5, 5);
				currentPiece = battleship;
				getChildren().add(battleship);
			}

		}

		public void placePieceOnBoard(double y, double x) {
			switch (currentPiece.type) {
			case 1:
				placedShip[0] = true;
				if (currentPiece.getRotate() == 0) {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					if (y % 80 > 40)
						row++;
					shipLocation[row][column] = 1;
					shipLocation[row - 1][column] = 1;
					currentPiece.setLayoutX(column * 80 + 40);
					currentPiece.setLayoutY(row * 80);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				} else {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					if (x % 80 > 40)
						column++;
					shipLocation[row][column] = 1;
					shipLocation[row][column - 1] = 1;
					currentPiece.setLayoutX(column * 80);
					currentPiece.setLayoutY(row * 80 + 40);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				}
				break;
			case 2:
				placedShip[1] = true;
				if (currentPiece.getRotate() == 0) {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					shipLocation[row][column] = 2;
					shipLocation[row - 1][column] = 2;
					shipLocation[row + 1][column] = 2;
					currentPiece.setLayoutX(column * 80 + 40);
					currentPiece.setLayoutY(row * 80 + 40);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				} else {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					shipLocation[row][column] = 2;
					shipLocation[row][column - 1] = 2;
					shipLocation[row][column + 1] = 2;
					currentPiece.setLayoutX(column * 80 + 40);
					currentPiece.setLayoutY(row * 80 + 40);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				}
				break;
			case 3:
				placedShip[2] = true;
				if (currentPiece.getRotate() == 0) {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					shipLocation[row][column] = 3;
					shipLocation[row - 1][column] = 3;
					shipLocation[row + 1][column] = 3;
					currentPiece.setLayoutX(column * 80 + 40);
					currentPiece.setLayoutY(row * 80 + 40);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				} else {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					shipLocation[row][column] = 3;
					shipLocation[row][column - 1] = 3;
					shipLocation[row][column + 1] = 3;
					currentPiece.setLayoutX(column * 80 + 40);
					currentPiece.setLayoutY(row * 80 + 40);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				}
				break;
			case 4:
				placedShip[3] = true;
				if (currentPiece.getRotate() == 0) {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					if (y % 80 > 40)
						row++;
					shipLocation[row][column] = 4;
					shipLocation[row - 1][column] = 4;
					shipLocation[row - 2][column] = 4;
					shipLocation[row + 1][column] = 4;
					currentPiece.setLayoutX(column * 80 + 40);
					currentPiece.setLayoutY(row * 80);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				} else {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					if (x % 80 > 40)
						column++;
					shipLocation[row][column] = 4;
					shipLocation[row][column - 1] = 4;
					shipLocation[row][column - 2] = 4;
					shipLocation[row][column + 1] = 4;
					currentPiece.setLayoutX(column * 80);
					currentPiece.setLayoutY(row * 80 + 40);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				}
				break;
			case 5:
				if (currentPiece.getRotate() == 0) {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					shipLocation[row][column] = 5;
					shipLocation[row - 1][column] = 5;
					shipLocation[row + 1][column] = 5;
					shipLocation[row - 2][column] = 5;
					shipLocation[row + 2][column] = 5;
					currentPiece.setLayoutX(column * 80 + 40);
					currentPiece.setLayoutY(row * 80 + 40);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				} else {
					int row = (int) (y / 80);
					int column = (int) (x / 80);
					shipLocation[row][column] = 5;
					shipLocation[row][column - 1] = 5;
					shipLocation[row][column + 1] = 5;
					shipLocation[row][column - 2] = 5;
					shipLocation[row][column + 2] = 5;
					currentPiece.setLayoutX(column * 80 + 40);
					currentPiece.setLayoutY(row * 80 + 40);
					currentPiece.row = row;
					currentPiece.column = column;
					printShipBoard();
				}
				placedShip[4] = true;
				break;
			}
		}

		private void printShipBoard() {
			for (int i = 0; i < 10; i++) {
				for (int k = 0; k < 10; k++) {
					System.out.print(shipLocation[i][k] + " ");
				}
				System.out.print("\n");
			}
		}
	}

	class BattleShip extends Ellipse {
		int type;
		boolean rotate = false;
		int length;
		int row;
		int column;

		BattleShip(int length, int type) {
			super(40, 40 * length);
			setFill(new Color(0, 1, 0, 1));
			setStroke(Color.BLACK);
			this.type = type;
			this.length = length;
			ships.add(this);
		}
	}

	class Marker extends Circle {
		Marker(int status, int row, int column) {
			super(40);
			setCenterX(column * 80 + 40);
			setCenterY(row * 80 + 40);
			if (status == HIT) {
				setFill(new Color(1, 0, 0, 0.8));
			} else if (status == MISS) {
				setFill(new Color(1, 1, 1, 0.8));
			}
		}
	}

}