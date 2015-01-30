package be.neutrinet.ispng.dns;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by wannes on 1/25/15.
 */
public class TCPServer implements Runnable {

    private InetAddress addr;
    private int port;
    private RequestHandler handler;
    private boolean run = true;

    public TCPServer(InetAddress addr, int port, RequestHandler handler) {
        this.addr = addr;
        this.port = port;
        this.handler = handler;
    }

    public void run() {
        try {
            ServerSocket sock = new ServerSocket(port, 128, addr);
            while (run) {
                final Socket s = sock.accept();
                Thread t;

                t = new Thread(new TCPClient(s, handler));
                t.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to open DNS TCP socket", ex);
        }
    }
}
