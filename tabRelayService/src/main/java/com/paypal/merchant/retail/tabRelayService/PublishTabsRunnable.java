package com.paypal.merchant.retail.tabRelayService;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.paypal.merchant.retail.sdk.contract.commands.CommandResult;
import com.paypal.merchant.retail.sdk.contract.commands.ErrorInfo;
import com.paypal.merchant.retail.sdk.contract.commands.GetTabsCommand;
import com.paypal.merchant.retail.sdk.contract.commands.GetTabsResponse;
import com.paypal.merchant.retail.sdk.contract.entities.Tab;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueue;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.IMessageQueueManager;
import com.paypal.merchant.retail.sdk.contract.plugins.socket.TabEvent;
import com.paypal.merchant.retail.sdk.internal.common.ConfigurationImpl;
import com.paypal.merchant.retail.utils.PropertyManager;
import com.paypal.merchant.retail.zeroMqMessaging.MessageQueueManager;
import org.apache.log4j.Logger;
import org.omg.CORBA.portable.ApplicationException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Paolo on 6/27/2014.
 */
public class PublishTabsRunnable implements Runnable {
    private static Logger logger = Logger.getLogger(PublishTabsRunnable.class);
    private ConfigurationImpl.Store store;
    private GetTabsCommand command;
    private IMessageQueue deltaQueue;
    private IMessageQueue stateQueue;
    private GetTabsResponse previousResponse;

    public PublishTabsRunnable(ConfigurationImpl.Store store, GetTabsCommand command) {
        this.store = store;
        this.command = command;

        // Get queue/socket for deltas (Added/Removed tabs)
        this.deltaQueue = MessageQueueManager.newInstance().getMessageQueue(PropertyManager.INSTANCE.getProperty("getTabsDelta.endpoint"),
                this.store.getLocationId(), IMessageQueueManager.ConnectionType.Publish);

        // Get queue/socket for current state (ALL tabs)
        this.stateQueue = MessageQueueManager.newInstance().getMessageQueue(PropertyManager.INSTANCE.getProperty("getTabsState.endpoint"),
                this.store.getLocationId(), IMessageQueueManager.ConnectionType.Publish);
    }

    /**
     * This will be executed at the scheduled interval
     */
    @Override
    public void run() {
        // Set the last modified if we have a previous response
        if(previousResponse != null) {
            logger.debug("Setting getTabsRequest lastModified: " + previousResponse.getLastModified());
            command.getRequest().setLastModified(previousResponse.getLastModified());
        }

        CommandResult result = command.execute();
        ErrorInfo errorInfo = command.getErrorInfo();
        logger.info(command.getClass().getSimpleName() + " Result: " + result.name());
        if (result != CommandResult.Success && errorInfo != null && errorInfo.getData().size() > 0) {
            logger.error(command.getClass().getSimpleName() + ": Error Code: " + errorInfo.getCode().name());
            logger.error(command.getClass().getSimpleName() + ": Error ID: " + errorInfo.getData().get(0).getErrorId());
            logger.error(command.getClass().getSimpleName() + ": Error Msg: " + errorInfo.getData().get(0).getMessage());
        } else {
            GetTabsResponse response = command.getResponse();
            publishTabs(response);
            previousResponse = response;
        }
    }

    /**
     * This will publish both the tab STATE and DELTAS.
     * STATE will be all current tabs for a location and DELTAS are the changes from the previous "run"
     * @param currentResponse The GetTabsResponse from the most current request
     */
    private void publishTabs(GetTabsResponse currentResponse) {
        if(logger.isDebugEnabled()){
            logger.debug("Number of tabs for location (" + command.getRequest().getLocationId() + "): " + currentResponse.getTabs().size());
            for(Tab tab : currentResponse.getTabs()) {
                logger.debug("Tab from SDK Response: " + tab.getId()+ " | Customer: " + tab.getCustomerName());
            }
        }

        // add all the tabs from the current SDK response
        Map<String, Tab> currentTabsMap = new HashMap<String, Tab>();
        for(Tab tab : currentResponse.getTabs()){
            currentTabsMap.put(tab.getId(), tab);
        }

        // add all the tabs from the previous SDK response (if it exists)
        Map<String, Tab> previousTabsMap = new HashMap<String, Tab>();
        if(previousResponse != null){
            for(Tab tab : previousResponse.getTabs()){
                previousTabsMap.put(tab.getId(), tab);
            }
        }

        try {
            // Send all current tabs to the current queue
            HashSet<TabEvent> tabState = new HashSet<TabEvent>();
            for(Tab tab : currentTabsMap.values()) {
                tabState.add(new TabEvent(this, IMessageQueue.TabEventType.Added, store.getLocationId(), tab));
            }
            logger.debug("Publishing ALL current tabs (" + tabState.size() + ") to stateQueue");
            this.stateQueue.send(tabState);
        } catch (ApplicationException e) {
            logger.error("Failed to publish tabs state", e);
        }

        try {
            // Send just the deltas to the delta queue
            HashSet<TabEvent> tabDeltas = getTabDeltas(previousTabsMap, currentTabsMap);
            logger.debug("Publishing DELTA tabs (" + tabDeltas.size() + ") to deltaQueue");
            this.deltaQueue.send(tabDeltas);
        } catch (ApplicationException e) {
            logger.error("Failed to publish tabs delta", e);
        }
    }

    /**
     * Return a set of tab deltas - added and removed
     * Explicitly using HashSet because it is serializable unlike the Set interface
     * @return - Set of tabs that have been added or removed
     */
    private HashSet<TabEvent> getTabDeltas(Map<String, Tab> previousTabs, Map<String, Tab> currentTabs) {

        if(logger.isDebugEnabled()){
            for(Tab tab : previousTabs.values()) {
                logger.debug("PREVIOUS TAB: Id:" + tab.getId() + " | Customer: " + tab.getCustomerName());
            }
            for(Tab tab : currentTabs.values()) {
                logger.debug("CURRENT TAB: Id:" + tab.getId() + " | Customer: " + tab.getCustomerName());
            }
        }

        // using guava library to get differences between previous and current
        HashSet<TabEvent> tabDeltas = new HashSet<TabEvent>();
        MapDifference<String, Tab> mapDifference = Maps.difference(previousTabs, currentTabs);
        Map<String, Tab> removedTabs = mapDifference.entriesOnlyOnLeft();
        Map<String, Tab> addedTabs = mapDifference.entriesOnlyOnRight();

        for(Tab tab : removedTabs.values()) {
            logger.debug("Removed Tab: " + tab.getId() + " | Customer: " + tab.getCustomerName());
            tabDeltas.add(new TabEvent(this, IMessageQueue.TabEventType.Removed, store.getLocationId(), tab));
        }

        for(Tab tab : addedTabs.values()) {
            logger.debug("Added Tab: " + tab.getId()+ " | Customer: " + tab.getCustomerName());
            tabDeltas.add(new TabEvent(this, IMessageQueue.TabEventType.Added, store.getLocationId(), tab));
        }

        logger.debug("Total number of deltas: " + tabDeltas.size());
        return tabDeltas;
    }
}
