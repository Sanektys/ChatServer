package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {

    private List<Client> clients;
    private ServerSocket serverSocket;

    public ChatServer() {
        // создаем серверный сокет на порту 1234
        try {
            serverSocket = new ServerSocket(1234);
            serverSocket.setSoTimeout(3000);  //Тут выступает как задержка перед чтением команд сервером
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) { new ChatServer().run(); }

    public void sendAll(String message) {
        for (var client : clients) {
            client.receive(message);
        }
    }

    public boolean sendTo(String toClientName, String message) {
        for (var client : clients) {
            if (client.getClientName().equalsIgnoreCase(toClientName)) {
                client.receive(message);
                return true;
            }
        }
        return false;
    }

    public boolean checkNameIsFree(String name) {
        for (var client : clients) {
            if (client.getClientName().equalsIgnoreCase(name)) {
                return false;
            }
        }
        return true;
    }

    public void run() {
        System.out.println("Server started. You can send command \"Stop\" or \"Restart\".");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String input = "start";
        Thread acceptor = null;
        while (input != null && !input.equalsIgnoreCase("stop")) {
            if (acceptor == null || input.equalsIgnoreCase("restart")) {
                if (acceptor != null && acceptor.isAlive()) {
                    acceptor.interrupt();  //При перезагрузке прерываем поток и ждём его завершения,
                    try {                  //потом создастся новый
                        acceptor.join();
                        System.out.println("...and restarted.");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                acceptor = new Thread(this::acceptClients);  //Клиенты подключаются в отдельном потоке
                acceptor.start();
            }
            try {
                input = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (input != null) {
                if (!input.equalsIgnoreCase("stop") && !input.equalsIgnoreCase("restart")) {
                    sendAll(String.format("<System>: %s", input));  //Если это не команды, то просто чат от имени системы
                }
            }
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        acceptor.interrupt();
        try {
            acceptor.join();
            serverSocket.close();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server stopped.");
    }

    private void acceptClients() {
        clients = new ArrayList<>();  //При перезагрузке массив клиентов обнуляется
        while (!Thread.interrupted()) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected!");

                clients.add(new Client(socket, this));
            } catch (SocketTimeoutException e) {
                continue;  //Таймаут для перехвата прерывания
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Disconnecting clients...");
        for (var client : clients) {
            client.disconnect();
        }
    }
}
