package Server;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class NetClient extends JFrame implements KeyListener {

	private final String serverIP = "127.0.0.1";
	private final int serverPort = 1234;

	private final JTextArea textArea;
	private BufferedReader in;
	private PrintWriter out;
	private Socket socket;
	private boolean isConnected;
	private boolean isTyping = false;
	private Thread inputThread;
	private final StringBuffer outputStringBuffer = new StringBuffer();
	private int lastKeyPressed;

	NetClient() {
		// ������� ����
		super("Simple Chat client");
		setSize(400, 500);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// ��������� �� ���� ��������� ����
		textArea = new JTextArea();
		textArea.setBackground(Color.BLACK);
		textArea.setForeground(Color.WHITE);
		textArea.setEditable(false);
		textArea.setMargin(new Insets(10, 10, 10, 10));
		this.add(new JScrollPane(textArea));

		// �������������� � �������
		connect();
	}

	public static void main(String[] args) { new NetClient().setVisible(true); }

	public void checkingConnectionLoop() {
		while (isConnected) {
			try {
				Thread.sleep(500);  // ������ ���������� ��������� ����������
			} catch (InterruptedException e) {
				break;
			}
		}
		if (textArea.getForeground() != Color.RED) {
			textArea.setForeground(Color.LIGHT_GRAY);
		}
		try {
			if (inputThread != null && inputThread.isAlive()) {
				inputThread.join();
			}
			if (socket != null) {
				in.close();
				out.close();
				socket.close();
			}
			Thread.sleep(2500);  // ������ �������� ����� ��������� ����
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	private void connect() {
		try {
			socket = new Socket(serverIP, serverPort);
			isConnected = true;
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());
			textArea.addKeyListener(this);
		} catch (IOException e) {
			isConnected = false;
			textArea.setForeground(Color.RED);
			textArea.append("Server " + serverIP + " port " + serverPort + " NOT AVAILABLE");
		}

		inputThread = new Thread(() -> {  // � ��������� ������ ��������� ������ �� �������
			String inputText;
			while (isConnected) {
				try {
					if (!isTyping && in.ready()) {  // ���� ������ ��������, �� �� �������� ����� ���������
						inputText = in.readLine();
						if (inputText == null) {  // ������ ������ ������ ����� ��� ������
							isConnected = false;
						} else {
							addStringToTextArea(inputText);
							addCharToTextArea('\n');
						}
					} else {
						in.mark(1);  // ���� ����� ���, ���������, �� �������� �� EOF
						if (in.read() == -1) {
							isConnected = false;
						} else {
							in.reset();
						}
						try {
							Thread.sleep(200);  // �������� ������ ������
						} catch (InterruptedException e) {
							break;
						}
					}
				} catch (IOException e) {
					isConnected = false;
					textArea.setForeground(Color.RED);
					textArea.append("\nCONNECTION ERROR");
					break;
				}
			}
		});
		inputThread.start();

		// ����� �������� ����������
		Thread connectionThread = new Thread(this::checkingConnectionLoop);
		connectionThread.start();
	}

	private void addStringToTextArea(String text) {
		textArea.append(text);
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}
	private void addCharToTextArea(char symbol) {
		textArea.append(Character.toString(symbol));
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}
	private void removeCharInTextArea() {
		textArea.replaceRange("", textArea.getDocument().getLength() - 1,
				              textArea.getDocument().getLength());
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	@Override
	public void keyPressed(KeyEvent key) {
		lastKeyPressed = key.getKeyCode();
	}
	@Override
	public void keyReleased(KeyEvent arg0) {}

	@Override
	public void keyTyped(KeyEvent key) {
		// �� ������� ������� ���������� �������� � ��������� ��������
		char keyChar = key.getKeyChar();
		String outputText;
		if (lastKeyPressed == KeyEvent.VK_ENTER) {
			if (!outputStringBuffer.isEmpty()) {
				outputText = outputStringBuffer.toString();
				outputStringBuffer.delete(0, outputStringBuffer.length());

				out.println(outputText);
				out.flush();

				addCharToTextArea(keyChar);
				System.out.println(outputText);

				if (outputText.equalsIgnoreCase("exit")) {  // ���� ����� exit ������ ���� �� ����������
					isConnected = false;
				}
			}
			isTyping = false;
		} else if (lastKeyPressed == KeyEvent.VK_BACK_SPACE) {
			if (!outputStringBuffer.isEmpty()) {
				outputStringBuffer.deleteCharAt(outputStringBuffer.length() - 1);
				removeCharInTextArea();
				if (outputStringBuffer.isEmpty()) {
					isTyping = false;
				}
			}
			// ������ ���� ��������� ������ ����� ESC, ALT, CTRL
		} else if (!(lastKeyPressed == KeyEvent.VK_ESCAPE || lastKeyPressed == KeyEvent.VK_ALT
			       || lastKeyPressed == KeyEvent.VK_CONTROL || lastKeyPressed == KeyEvent.VK_WINDOWS)) {
			outputStringBuffer.append(keyChar);
			addCharToTextArea(keyChar);
			isTyping = true;
		}
	}
}