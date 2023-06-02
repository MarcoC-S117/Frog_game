package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class FrogGame extends JPanel implements KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int WINDOW_WIDTH = 400;
	private static final int WINDOW_HEIGHT = 600;

	private ImageIcon[] carIcons;
	private ImageIcon frogIcon;
	private ImageIcon metaIcon;
	private ImageIcon wallIcon;
	private int[] carX;
	private boolean[] carMovingLeft;
	private int frogX;
	private int frogY;
	private int moveVelFrog = 5;
	private int score = 0;
	private int carSpeed = 3; // Velocidad de movimiento de los autos
	private int carSpacing = 100; // Separación entre los autos
	private ImageIcon[] trunkIcon;
	private int[] trunkX;
	private int[] trunkY;
	private boolean[] trunkMovingLeft;
	private int trunkSpeed = 2;
	private boolean collisionDetectionRunning;

	public FrogGame() {
		setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
		setFocusable(true);
		addKeyListener(this);

		carIcons = new ImageIcon[6]; // Cambiar a 6 autos
		for (int i = 0; i < carIcons.length; i++) {
			carIcons[i] = new ImageIcon("src/car.png"); // Ruta de la imagen del carrito
		}

		frogIcon = new ImageIcon("src/frog.png"); // Ruta de la imagen de la rana
		metaIcon = new ImageIcon("src/end_line.png"); // Ruta de la imagen de la línea de meta
		wallIcon = new ImageIcon("src/wall.png");
		trunkIcon = new ImageIcon[6]; // Cambiar a 6 troncos
		for (int i = 0; i < trunkIcon.length; i++) {
			trunkIcon[i] = new ImageIcon("src/trunk.png");// Ruta de la imagen del tronco
		}

		carX = new int[carIcons.length];
		carMovingLeft = new boolean[carIcons.length];
		for (int i = 0; i < carX.length; i++) {
			carX[i] = i * carSpacing; // Cambiar el cálculo de la posición x del auto
			carMovingLeft[i] = (i % 2 == 0); // Alternar la dirección de los carros
		}

		frogX = WINDOW_WIDTH / 2 - frogIcon.getIconWidth() / 2;
		frogY = WINDOW_HEIGHT - frogIcon.getIconHeight() - 10;

		trunkX = new int[trunkIcon.length];
		trunkY = new int[trunkIcon.length];
		trunkMovingLeft = new boolean[trunkIcon.length];
		for (int i = 0; i < trunkIcon.length; i++) {
			trunkX[i] = i * carSpacing; // Cambiar el cálculo de la posición x del tronco
			trunkY[i] = 40 + (i * 40); // Cambiar el cálculo de la posición y del tronco
			trunkMovingLeft[i] = (i % 2 == 0); // Alternar la dirección de los troncos
		}

		// Iniciar hilos de movimiento de troncos
		for (int i = 0; i < trunkIcon.length; i++) {
			int trunkIndex = i;
			Thread trunkThread = new Thread(() -> {
				while (true) {
					moveTrunk(trunkIndex);
					try {
						Thread.sleep(30);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			trunkThread.start();
		}

		// Iniciar hilos de movimiento de carros
		for (int i = 0; i < carIcons.length; i++) {
			int carIndex = i;
			Thread carThread = new Thread(() -> {
				while (true) {
					moveCar(carIndex);
					try {
						Thread.sleep(30);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			carThread.start();
		}
		collisionDetectionRunning = true;

		// Iniciar hilo de detección de colisiones
		Thread collisionDetectionThread = new Thread(() -> {
			while (collisionDetectionRunning) {
				checkCollision();
				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		collisionDetectionThread.start();
		try {
	        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("src/music.wav").getAbsoluteFile());
	        Clip clip = AudioSystem.getClip();
	        clip.open(audioInputStream);
	        clip.start();
	       } catch(UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
	         System.out.println("Error al reproducir el sonido.");
	       }
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Dibujar fondo gris
		g.setColor(Color.GRAY);
		g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

		// Dibujar fondo azul desde el pixel 276 hacia arriba
		g.setColor(Color.BLUE);
		g.fillRect(0, 0, WINDOW_WIDTH, 276);

		// Dibujar línea de meta
		int metaWidth = 50;
		int numMetaIcons = WINDOW_WIDTH / metaWidth;
		for (int i = 0; i < numMetaIcons; i++) {
			int metaX = i * metaWidth;
			metaIcon.paintIcon(this, g, metaX, 0);
		}

		int wallY = WINDOW_HEIGHT / 2 - wallIcon.getIconHeight() / 2;
		int numWallIcons = WINDOW_WIDTH / wallIcon.getIconWidth();
		for (int i = 0; i < numWallIcons; i++) {
			int wallX = i * wallIcon.getIconWidth();
			wallIcon.paintIcon(this, g, wallX, wallY);
		}

		// Dibujar carros
		for (int i = 0; i < carIcons.length; i++) {
			carIcons[i].paintIcon(this, g, carX[i], 320 + (i * 40));
		}

		// Dibujar troncos
		for (int i = 0; i < trunkIcon.length; i++) {
			trunkIcon[i].paintIcon(this, g, trunkX[i], 40 + (i * 40));
		}

		// Dibujar rana
		frogIcon.paintIcon(this, g, frogX, frogY);

		// Dibujar marcador
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.BOLD, 20));
		g.drawString("Score: " + score, 10, 80);
	}

	public synchronized void moveCar(int carIndex) {
		if (carMovingLeft[carIndex]) {
			carX[carIndex] = (carX[carIndex] - carSpeed + WINDOW_WIDTH) % WINDOW_WIDTH;
		} else {
			carX[carIndex] = (carX[carIndex] + carSpeed) % WINDOW_WIDTH;
		}
		repaint();
	}

	public synchronized void moveTrunk(int trunkIndex) {
		if (trunkMovingLeft[trunkIndex]) {
			trunkX[trunkIndex] = (trunkX[trunkIndex] - trunkSpeed + WINDOW_WIDTH) % WINDOW_WIDTH;
		} else {
			trunkX[trunkIndex] = (trunkX[trunkIndex] + trunkSpeed) % WINDOW_WIDTH;
		}
		repaint();
	}

	public synchronized void moveFrog(int deltaX, int deltaY) {
		int newFrogX = frogX + deltaX;
		int newFrogY = frogY + deltaY;

		if (newFrogX >= 0 && newFrogX <= WINDOW_WIDTH - frogIcon.getIconWidth()) {
			frogX = newFrogX;
		}

		if (newFrogY >= 0 && newFrogY <= WINDOW_HEIGHT - frogIcon.getIconHeight()) {
			frogY = newFrogY;
		}

		repaint();
	}

	public synchronized void checkCollision() {
	    Rectangle frogRect = new Rectangle(frogX, frogY, frogIcon.getIconWidth(), frogIcon.getIconHeight());

	    boolean onTrunk = false; // Variable para indicar si la rana está sobre un tronco

	    for (int i = 0; i < trunkIcon.length; i++) {
	        Rectangle trunkRect = new Rectangle(trunkX[i], 40 + (i * 40), trunkIcon[i].getIconWidth(),
	                trunkIcon[i].getIconHeight());
	        if (frogRect.intersects(trunkRect)) {
	            // La rana está sobre un tronco, ajustar su posición para que se mueva con el tronco
	            if (trunkMovingLeft[i]) {
	                moveFrog(-trunkSpeed, 0);
	            } else {
	                moveFrog(trunkSpeed, 0);
	            }
	            onTrunk = true;
	            break;
	        }
	    }

	    // Verificar colisión con el agua solo si la rana no está sobre un tronco
	    if (!onTrunk) {
	        Rectangle waterRect = new Rectangle(0, 100, WINDOW_WIDTH, 175);
	        if (frogRect.intersects(waterRect)) {
	            score = 0;
	            resetGame(); // Reiniciar el juego
	            return;
	        }
	    }

	    // Verificar colisión con los carros
	    for (int i = 0; i < carIcons.length; i++) {
	        Rectangle carRect = new Rectangle(carX[i], 320 + (i * 40), carIcons[i].getIconWidth(),
	                carIcons[i].getIconHeight());

	        if (frogRect.intersects(carRect)) {
	            score = 0;
	            resetGame(); // Reiniciar el juego
	            return;
	        }
	    }

	    // Verificar colisión con la línea de meta si no se ha producido una colisión con un carro
	    Rectangle metaRect = new Rectangle(0, 0, WINDOW_WIDTH, 50);
	    if (frogRect.intersects(metaRect)) {
	        score++;
	        resetGame(); // Reiniciar el juego
	        return;
	    }
	}


	public synchronized void resetGame() {
		for (int j = 0; j < carX.length; j++) {
			int minPosition = WINDOW_HEIGHT - 275;
			int maxPosition = WINDOW_HEIGHT - carIcons[j].getIconHeight() - 50;
			carX[j] = (int) (minPosition + Math.random() * (maxPosition - minPosition));
			carMovingLeft[j] = (j % 2 == 0);
		}

		for (int i = 0; i < trunkX.length; i++) {
			int minPosition = WINDOW_HEIGHT - 275;
			int maxPosition = WINDOW_HEIGHT - trunkIcon[i].getIconHeight() - 50;
			trunkX[i] = (int) (minPosition + Math.random() * (maxPosition - minPosition));
			trunkMovingLeft[i] = (i % 2 == 0);
		}

		frogX = WINDOW_WIDTH / 2 - frogIcon.getIconWidth() / 2;
		frogY = WINDOW_HEIGHT - frogIcon.getIconHeight() - 10;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_LEFT) {
			moveFrog(-1 * moveVelFrog, 0);
		} else if (keyCode == KeyEvent.VK_RIGHT) {
			moveFrog(moveVelFrog, 0);
		} else if (keyCode == KeyEvent.VK_UP) {
			moveFrog(0, -1 * moveVelFrog);
		} else if (keyCode == KeyEvent.VK_DOWN) {
			moveFrog(0, moveVelFrog);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Frog Game");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.add(new FrogGame());
		frame.pack();
		frame.setVisible(true);
	}
}
