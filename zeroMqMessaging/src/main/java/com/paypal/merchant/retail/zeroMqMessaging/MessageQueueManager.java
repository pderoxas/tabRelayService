package com.paypal.merchant.retail.zeroMqMessaging;

import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueue;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueueManager;

public class MessageQueueManager implements IMessageQueueManager {

	private MessageQueueManager() {
	}

	public static IMessageQueueManager newInstance() {
		return new MessageQueueManager();
	}

	@Override
	public IMessageQueue getMessageQueue(String endpoint, String subject, ConnectionType type) {
		switch (type) {
		case Publish:
			return new Publisher(endpoint, subject);
		case Subscribe:
			return new Subscriber(endpoint, subject);
		default:
			return null;
		}
	}
}
