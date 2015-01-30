package be.neutrinet.ispng.dns;

import org.apache.log4j.Logger;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by wannes on 1/25/15.
 */
public class UDPServer implements Runnable {

    private boolean run = true;
    private InetAddress addr;
    private int port;
    private RequestHandler handler;

    public UDPServer(InetAddress addr, int port, RequestHandler handler) {
        this.addr = addr;
        this.port = port;
        this.handler = handler;
    }

    public void stop() {
        run = false;
    }

    @Override
    public void run() {
        try {
            DatagramSocket sock = new DatagramSocket(port, addr);
            final short udpLength = 512;
            byte[] in = new byte[udpLength];
            DatagramPacket indp = new DatagramPacket(in, in.length);
            DatagramPacket outdp = null;
            while (run) {
                indp.setLength(in.length);
                try {
                    sock.receive(indp);
                } catch (InterruptedIOException e) {
                    continue;
                }

                Message query;
                byte[] response = null;

                try {
                    query = new Message(in);
                    response = handler.generateReply(query, in, indp.getLength(), null);

                    if (response == null)
                        continue;
                } catch (IOException e) {
                    response = handler.formerrMessage(in);
                }

                if (outdp == null)
                    outdp = new DatagramPacket(response, response.length, indp.getAddress(), indp.getPort());
                else {
                    outdp.setData(response);
                    outdp.setLength(response.length);
                    outdp.setAddress(indp.getAddress());
                    outdp.setPort(indp.getPort());
                }

                sock.send(outdp);
            }
        } catch (IOException e) {
            Logger.getLogger(getClass()).error("Failed to service request", e);
        }
    }
}
