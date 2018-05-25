# Java-Game
# Noughts and Crosses

package com.JKapp.marvelXO;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;

public class MarvelXO implements Runnable {

	private String ip = "localhost";
	private int port = 1234;
	private Scanner scanner = new Scanner(System.in);
	private JFrame frame;
	private final int WIDTH = 506;
	private final int HEIGHT = 570;
	private Thread thread;

	private Painter painter;
	private Socket socket;
	private DataOutputStream DataOutStream;
	private DataInputStream DataInStream;

	private ServerSocket serverSocket;

	private BufferedImage layout;
	private BufferedImage rX;
	private BufferedImage bX;
	private BufferedImage rO;
	private BufferedImage bO;

	private String[] Block = new String[9];

	private boolean yourTurn = false;
	private boolean circle = true;
	private boolean acc = false;
	private boolean OppCommFailed = false;
	private boolean won = false;
	private boolean enemyWon = false;
	private boolean draw = false;

	private int SPLength = 160;
	private int errors = 0;
	private int Spot1 = -1;
	private int Spot2 = -1;

	private Font font = new Font("Ubuntu", Font.BOLD, 30);

	private String waitingString = "Waiting for another player";
	private String OppCommFailedSTRING = "Unable to communicate with opponent.";
	private String wonString = "You won!";
	private String enemyWonString = "Opponent won!";
	private String tieString = "Game ended in a draw.";

	private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };


	public MarvelXO() {
		
		ip = JOptionPane.showInputDialog("Please input the IP: ");

		loadImages();

		painter = new Painter();
		painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

		if (!connect()) initializeServer();

		frame = new JFrame();
		frame.setTitle("Marvel XO");
		frame.setContentPane(painter);
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);

		thread = new Thread(this, "MarvelXO");
		thread.start();
	}

	public void run() {
		while (true) {
			tick();
			painter.repaint();

			if (!circle && !acc) {
				listenForServerRequest();
			}

		}
	}
	
	

	private void render(Graphics g) {
		g.drawImage(layout, 0, 0, null);
		if (OppCommFailed) {
			g.setColor(Color.RED);
			g.setFont(font);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(OppCommFailedSTRING);
			g.drawString(OppCommFailedSTRING, WIDTH / 2 - stringWidth / 2, HEIGHT - 36);
			return;
		}

		if (acc) {
			for (int i = 0; i < Block.length; i++) {
				if (Block[i] != null) {
					if (Block[i].equals("X")) {
						if (circle) {
							g.drawImage(rX, (i % 3) * SPLength + 10 * (i % 3), (int) (i / 3) * SPLength + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(bX, (i % 3) * SPLength + 10 * (i % 3), (int) (i / 3) * SPLength + 10 * (int) (i / 3), null);
						}
					} else if (Block[i].equals("O")) {
						if (circle) {
							g.drawImage(bO, (i % 3) * SPLength + 10 * (i % 3), (int) (i / 3) * SPLength + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(rO, (i % 3) * SPLength + 10 * (i % 3), (int) (i / 3) * SPLength + 10 * (int) (i / 3), null);
						}
					}
				}
			}
			if (won || enemyWon) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new BasicStroke(10));
				g.setColor(Color.YELLOW);
				g.drawLine(Spot1 % 3 * SPLength + 10 * Spot1 % 3 + SPLength / 2, (int) (Spot1 / 3) * SPLength + 10 * (int) (Spot1 / 3) + SPLength / 2, Spot2 % 3 * SPLength + 10 * Spot2 % 3 + SPLength / 2, (int) (Spot2 / 3) * SPLength + 10 * (int) (Spot2 / 3) + SPLength / 2);

				g.setColor(Color.YELLOW);
				g.setFont(font);
				if (won) {
					int stringWidth = g2.getFontMetrics().stringWidth(wonString);
					g.drawString(wonString, WIDTH / 2 - stringWidth / 2, HEIGHT - 36);
				} else if (enemyWon) {
					int stringWidth = g2.getFontMetrics().stringWidth(enemyWonString);
					g.drawString(enemyWonString, WIDTH / 2 - stringWidth / 2, HEIGHT - 36);
				}
			}
			if (draw) {
				Graphics2D g2 = (Graphics2D) g;
				g.setColor(Color.YELLOW);
				g.setFont(font);
				int stringWidth = g2.getFontMetrics().stringWidth(tieString);
				g.drawString(tieString, WIDTH / 2 - stringWidth / 2, HEIGHT - 36);
			}
		} else {
			g.setColor(Color.YELLOW);
			g.setFont(font);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
			g.drawString(waitingString, WIDTH / 2 - stringWidth / 2, HEIGHT - 36);
		}

	}

	private void tick() {
		if (errors >= 10) OppCommFailed = true;

		if (!yourTurn && !OppCommFailed) {
			try {
				int space = DataInStream.readInt();
				if (circle) Block[space] = "X";
				else Block[space] = "O";
				checkForEnemyWin();
				checkForTie();
				yourTurn = true;
			} catch (IOException e) {
				e.printStackTrace();
				errors++;
			}
		}
	}

	private void checkForWin() {
		for (int i = 0; i < wins.length; i++) {
			if (circle) {
				if (Block[wins[i][0]] == "O" && Block[wins[i][1]] == "O" && Block[wins[i][2]] == "O") {
					Spot1 = wins[i][0];
					Spot2 = wins[i][2];
					won = true;
					String retry = JOptionPane.showInputDialog("Would you like to restart? (Y/N)");
				}
			} else {
				if (Block[wins[i][0]] == "X" && Block[wins[i][1]] == "X" && Block[wins[i][2]] == "X") {
					Spot1 = wins[i][0];
					Spot2 = wins[i][2];
					won = true;
				}
			}
		}
	}

	private void checkForEnemyWin() {
		for (int i = 0; i < wins.length; i++) {
			if (circle) {
				if (Block[wins[i][0]] == "X" && Block[wins[i][1]] == "X" && Block[wins[i][2]] == "X") {
					Spot1 = wins[i][0];
					Spot2 = wins[i][2];
					enemyWon = true;
				}
			} else {
				if (Block[wins[i][0]] == "O" && Block[wins[i][1]] == "O" && Block[wins[i][2]] == "O") {
					Spot1 = wins[i][0];
					Spot2 = wins[i][2];
					enemyWon = true;
				}
			}
		}
	}

	private void checkForTie() {
		for (int i = 0; i < Block.length; i++) {
			if ((Block[i] == null) && ((!won)||(!enemyWon))) {
				return;
			}
		}
		draw = true;
		if (won ||enemyWon)
		{
			draw = false;
		}
	}

	private void listenForServerRequest() {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			DataOutStream = new DataOutputStream(socket.getOutputStream());
			DataInStream = new DataInputStream(socket.getInputStream());
			acc = true;
			System.out.println("CLIENT HAS REQUESTED TO JOIN, AND WE HAVE acc");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean connect() {
		try {
			socket = new Socket(ip, port);
			DataOutStream = new DataOutputStream(socket.getOutputStream());
			DataInStream = new DataInputStream(socket.getInputStream());
			acc = true;
		} catch (IOException e) {
			System.out.println("Unable to connect to the address: " + ip + ":" + port + " | Starting a server");
			return false;
		}
		System.out.println("Successfully connected to the server.");
		return true;
	}

	private void initializeServer() {
		try {
			serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
		yourTurn = true;
		circle = false;
	}

	private void loadImages() {
		try {
			layout = ImageIO.read(getClass().getResourceAsStream("/board.png"));
			rX = ImageIO.read(getClass().getResourceAsStream("/redX.png"));
			rO = ImageIO.read(getClass().getResourceAsStream("/redCircle.png"));
			bX = ImageIO.read(getClass().getResourceAsStream("/blueX.png"));
			bO = ImageIO.read(getClass().getResourceAsStream("/blueCircle.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		MarvelXO marvelXO = new MarvelXO();
	}

	private class Painter extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;

		public Painter() {
			setFocusable(true);
			requestFocus();
			setBackground(Color.BLACK);
			addMouseListener(this);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			render(g);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (acc) {
				if (yourTurn && !OppCommFailed && !won && !enemyWon) {
					int x = e.getX() / SPLength;
					int y = e.getY() / SPLength;
					y *= 3;
					int position = x + y;

					if (Block[position] == null) {
						if (!circle) Block[position] = "X";
						else Block[position] = "O";
						yourTurn = false;
						repaint();
						Toolkit.getDefaultToolkit().sync();

						try {
							DataOutStream.writeInt(position);
							DataOutStream.flush();
						} catch (IOException e1) {
							errors++;
							e1.printStackTrace();
						}

						System.out.println("DATA WAS SENT");
						checkForWin();
						checkForTie();

					}
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {

		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

	}

}
