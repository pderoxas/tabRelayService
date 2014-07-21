package com.paypal.merchant.retail.zeroMqMessaging;

import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueue;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.TabEvent;
import org.omg.CORBA.portable.ApplicationException;
import org.zeromq.ZMQ;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;

public class Publisher implements IMessageQueue {
    private ZMQ.Context context;
    private ZMQ.Socket socket;
    private String subject;
    private String endpoint;

    public Publisher(String endpoint, String subject) {
        this.subject = subject;
        this.endpoint = endpoint;
        this.context = ZMQ.context(1);
        this.socket = context.socket(ZMQ.PUB);
        this.socket.bind(endpoint);
        this.socket.setLinger(5000);
        this.socket.setSndHWM(0);
    }

    @Override
    public void send(HashSet<TabEvent> tabs) throws ApplicationException {
        try {
            // Send message envelope (subject and content)
            socket.sendMore(subject);       // send subject
            socket.send(toByteArray(tabs)); // send content
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public HashSet<TabEvent> receive() {
        throw new UnsupportedOperationException("Invalid operation for Publisher.");
    }

    @Override
    public void disconnect() throws ApplicationException {
        try {
            // disconnect from the socket
            socket.disconnect(this.endpoint);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Convert (serialize) the object to a byte[]
     * @param obj The object to convert to byte[]
     * @return Byte array
     * @throws IOException
     */
    private static byte[] toByteArray(Serializable obj) throws IOException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
