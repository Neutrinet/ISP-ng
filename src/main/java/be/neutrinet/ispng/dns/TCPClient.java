package be.neutrinet.ispng.dns;

import org.apache.log4j.Logger;
import org.xbill.DNS.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by wannes on 1/25/15.
 */
public class TCPClient implements Runnable {

    private Socket socket;
    private RequestHandler handler;

    public TCPClient(Socket socket, RequestHandler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    public void run() {

        int inLength;
        DataInputStream dataIn;
        DataOutputStream dataOut;
        byte[] in;

        try {

            InputStream is = socket.getInputStream();
            dataIn = new DataInputStream(is);
            inLength = dataIn.readUnsignedShort();
            in = new byte[inLength];
            dataIn.readFully(in);

            Message query;
            byte[] response = null;
            try {
                query = new Message(in);
                response = handler.generateReply(query, in, in.length, socket);
                if (response == null)
                    return;
            } catch (IOException e) {
                response = handler.formerrMessage(in);
            }
            dataOut = new DataOutputStream(socket.getOutputStream());
            dataOut.writeShort(response.length);
            dataOut.write(response);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to service DNS client", ex);
        } finally {
            try {
                socket.close();
            } catch (Exception xe) {

            }
        }
    }
}
