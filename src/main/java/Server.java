import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private List<String> validPaths = List.of("/somefile1.html", "/somefile2.png", "/somefile3.png");
    private ServerSocket server;
    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void listen(int port) {
        try {
            server = new ServerSocket(port);
            while (true) {
                Socket socket = server.accept();
                Connection connection = new Connection(socket);
                executorService.submit(connection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    public void addHandler(String method, String path, Handler handler) {
//
//    }

    private class Connection implements Runnable {
        private Socket socket;
        BufferedReader in;
        BufferedOutputStream out;

        public Connection(Socket socket) throws IOException {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
        }

        private void close() {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                if (parts.length != 3) {
                    socket.close();
                }

                final var path = parts[1];
                if (!validPaths.contains(path)) {
                    out.write((
                            "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Lengh: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                    socket.close();
                }

                final var filePath = Path.of(".", "somedir", path);
                final var mimeType = Files.probeContentType(filePath);
                final var lengh = Files.size(filePath);
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Lengh: " + lengh + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, out);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close();
            }
        }
    }

    public void setValidPath(String validPath) {
        validPaths.add(validPath);
    }
}
