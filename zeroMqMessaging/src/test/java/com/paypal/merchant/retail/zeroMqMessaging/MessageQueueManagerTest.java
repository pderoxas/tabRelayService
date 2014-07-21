package com.paypal.merchant.retail.zeroMqMessaging;

import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueue;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueueManager;
import org.junit.Before;
import org.junit.Test;
import static junit.framework.TestCase.assertTrue;

public class MessageQueueManagerTest {
    private IMessageQueueManager messageQueueManager;

    @Before
    public void setUp() throws Exception {
        messageQueueManager = MessageQueueManager.newInstance();
    }

    @Test
    public void testNewInstance() throws Exception {
        IMessageQueueManager testMessageQueueManager = MessageQueueManager.newInstance();
        assertTrue(testMessageQueueManager instanceof MessageQueueManager);
    }

    @Test (expected = UnsupportedOperationException.class)
    public void testUnsupportedOperationException() throws Exception {
        IMessageQueue messageQueue = messageQueueManager.getMessageQueue("http://127.0.0.1:5000", "UnitTest", IMessageQueueManager.ConnectionType.Publish);
    }

    @Test
    public void testGetMessageQueuePublisher() throws Exception {
        IMessageQueue messageQueue = messageQueueManager.getMessageQueue("tcp://127.0.0.1:5000", "UnitTest", IMessageQueueManager.ConnectionType.Publish);
        assertTrue(messageQueue instanceof Publisher);
        messageQueue.disconnect();
    }

    @Test
    public void testGetMessageQueueSubscriber() throws Exception {
        IMessageQueue messageQueue = messageQueueManager.getMessageQueue("tcp://127.0.0.1:5000", "UnitTest", IMessageQueueManager.ConnectionType.Subscribe);
        assertTrue(messageQueue instanceof Subscriber);
        messageQueue.disconnect();
    }
}