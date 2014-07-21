package com.paypal.merchant.retail.zeroMqMessaging;

import com.paypal.merchant.retail.sdk.contract.entities.Tab;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueue;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueueManager;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.TabEvent;
import com.paypal.merchant.retail.sdk.internal.entities.TabImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;

public class PublisherTest {
    private IMessageQueueManager messageQueueManager;
    private IMessageQueue publisher;
    private IMessageQueue subscriber;
    private HashSet<TabEvent> tabEvents;

    @BeforeClass
    public void setUp() throws Exception {
        messageQueueManager = MessageQueueManager.newInstance();
        publisher = messageQueueManager.getMessageQueue("tcp://localhost:6000", "LocationId", IMessageQueueManager.ConnectionType.Publish);
        subscriber = messageQueueManager.getMessageQueue("tcp://localhost:6000", "LocationId", IMessageQueueManager.ConnectionType.Subscribe);

        Tab tab01 = new TabImpl();
        tab01.setID("tab_01");
        tab01.setCustomerName("Customer Number One");
        TabEvent tabEvent01 = new TabEvent(this, IMessageQueue.TabEventType.Added, "LocationId", tab01);

        Tab tab02 = new TabImpl();
        tab01.setID("tab_02");
        tab01.setCustomerName("Customer Number Two");
        TabEvent tabEvent02 = new TabEvent(this, IMessageQueue.TabEventType.Removed, "LocationId", tab02);

        Tab tab03 = new TabImpl();
        tab01.setID("tab_03");
        tab01.setCustomerName("Customer Number Three");
        TabEvent tabEvent03 = new TabEvent(this, IMessageQueue.TabEventType.Added, "DifferentLocationId", tab03);

        tabEvents = new HashSet<TabEvent>();
        tabEvents.add(tabEvent01);
        tabEvents.add(tabEvent02);
        tabEvents.add(tabEvent03);
    }

    @AfterClass
    public void tearDown() throws Exception {
        publisher.disconnect();
        subscriber.disconnect();
    }

    @Test
    public void testSend() throws Exception {
        publisher.send(tabEvents);

    }

    @Test
    public void testReceive() throws Exception {

    }

    @Test
    public void testDisconnect() throws Exception {

    }
}