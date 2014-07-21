package com.paypal.merchant.retail.zeroMqMessaging;

import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueue;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.TabEvent;
import org.omg.CORBA.portable.ApplicationException;
import org.zeromq.ZMQ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;

import static org.zeromq.ZMQ.*;

public class Subscriber implements IMessageQueue {
    private Context context;
    private ZMQ.Socket socket;
    private String subject;
    private String endpoint;


    public Subscriber(String endpoint, String subject) {
        this.endpoint = endpoint;
        this.subject = subject;

        System.out.println("creating context...");
        this.context = context(1);

        System.out.println("creating socket");
        this.socket = context.socket(SUB);

        System.out.println("Connecting to endpoint: " + endpoint);
        this.socket.connect(endpoint);

        System.out.println("Subscribing to subject: " + subject);
        this.socket.subscribe(subject.getBytes());
    }

    @Override
    public void send(HashSet<TabEvent> tabs) {
        throw new UnsupportedOperationException(
                "Invalid operation for Subscriber.");

    }

    @Override
    public HashSet<TabEvent> receive() throws ClassNotFoundException, IOException {
        try {
            // Read the message subject
            String subject = socket.recvStr();

            // Read message contents
            HashSet<TabEvent> tabEvents= fromBytes(socket.recv());
            if (tabEvents != null && tabEvents.size() > 0) {
                System.out.println("Receiving message from subject: " + subject);
                return tabEvents;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Unsubscribe and Disconnect
     * @throws ApplicationException
     */
    @Override
    public void disconnect() throws ApplicationException {
        socket.unsubscribe(subject.getBytes());
        socket.disconnect(this.endpoint);
    }

    /**
     * Deserialize from byte array
     *
     * @param tabBytes The byte array content from message queue
     * @return Deserialized byte array into HashSet
     * @throws IOException IOException
     * @throws ClassNotFoundException ClassNotFoundException
     */
    private static HashSet<TabEvent> fromBytes(byte[] tabBytes) throws IOException, ClassNotFoundException {
        if (tabBytes != null) {
            ObjectInputStream objectInputStream = new ObjectInputStream(
                    new ByteArrayInputStream(tabBytes));
            HashSet<TabEvent> tabs = (HashSet<TabEvent>) objectInputStream.readObject();
            objectInputStream.close();
            return tabs;
        }
        return null;
    }
}
