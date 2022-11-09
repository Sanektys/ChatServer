package Server;

import java.io.*;
import java.net.Socket;

public class Client implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private PrintStream out;
    private String clientName = "Anonymous";
    private boolean isClientIntroduced = false;

    public Client(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;

        new Thread(this).start();
    }

    public String getClientName() { return clientName; }

    public void receive(String message) {
        if (socket.isClosed()) {
            return;
        }
        if (isClientIntroduced && out != null) {
            out.println(message);
        }
    }

    void disconnect() {
        if (socket.isClosed()) {
            return;
        }
        if (out != null) {
            out.println("<System>: Server Stopped.");
            out.flush();
        }
        try {
            Thread.sleep(500); // просто задержка перед закрытием потоков
            socket.shutdownInput();  // для отключения клиента посылаем EOF в поток, потом input в run станет null
            socket.shutdownOutput();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
            // получаем потоки ввода и вывода
            OutputStream os = socket.getOutputStream();
            out = new PrintStream(os);

            // клиент задаёт свой ник
            out.println("Welcome to chat, introduce yourself:");
            String input = clientName;
            while (!isClientIntroduced) {
                input = in.readLine();
                if (input != null) {
                    input = input.trim();
                    if (input.isEmpty()) {
                        out.println("The empty nickname are consider as \"anonymous\".");
                        isClientIntroduced = true;
                    } else if (input.contains(" ")) {
                        out.println("Whitespaces are not allowed in the nickname, try another one:");
                    } else if (input.equalsIgnoreCase("system")
                            || input.equalsIgnoreCase("help")
                            || input.equalsIgnoreCase("null")) {
                        out.println("This nickname is unacceptable, try another one:");
                    } else if (!server.checkNameIsFree(input)) {
                        out.println("This nickname is already in use, try another one:");
                    } else {
                        clientName = input;
                        isClientIntroduced = true;
                    }
                } else {
                    break;
                }
            }  // теперь можно получать сообщения

            // цикл чата
            if (input != null && !input.equalsIgnoreCase("exit")) {
                out.printf("Hello %s, enjoy with conversation!%nFor exit type \"exit\"%n", clientName);
                input = in.readLine();
            }
            while (input != null && !input.equalsIgnoreCase("exit")) {
                input = input.trim();
                if (!input.isEmpty()) {

                    if (input.charAt(0) == '@') {  // прямое сообщение
                        String[] dividedMessage = input.split(" ", 2);
                        if (dividedMessage.length == 2) {
                            boolean isSenderAnon     = clientName.equalsIgnoreCase("anonymous");
                            boolean isReceiverAnon   = dividedMessage[0].equalsIgnoreCase("@anonymous");
                            boolean isReceiverSystem = dividedMessage[0].equalsIgnoreCase("@system");
                            boolean isReceiverHelp   = dividedMessage[0].equalsIgnoreCase("@help");
                            boolean isReceiverNull   = dividedMessage[0].equalsIgnoreCase("@null");

                            if (!isSenderAnon && !isReceiverAnon && !isReceiverSystem
                                    && !isReceiverHelp && !isReceiverNull) {
                                if (!server.sendTo(dividedMessage[0].replaceFirst("@", ""),
                                                   String.format("(to you) <%s>: %s", clientName, dividedMessage[1]))) {
                                    out.println("<help>: There is no such user");
                                }
                            } else {
                                if (isSenderAnon) {
                                    out.println("<help>: Anonymous can't talk with another person directly.");
                                } else if (isReceiverAnon) {
                                    out.println("<help>: You can't talk with anonymous directly.");
                                } else if (isReceiverNull) {
                                    out.println("<help>: You can't talk to a user who doesn't exist.");
                                } else {
                                    out.println("<help>: You can't send message to system.");
                                }
                            }
                        } else {
                            out.println("<help>: You didn't enter a message to the user.");
                        }
                    } else {  // сообщение всем (и себе)
                        server.sendAll(String.format("<%s>: %s", clientName, input));
                    }
                }
                input = in.readLine();
            }
            clientName = "null";
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Message receive from user was interrupted.");
            clientName = "null";
            out.close();
            try {
                socket.close();
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }
    }
}