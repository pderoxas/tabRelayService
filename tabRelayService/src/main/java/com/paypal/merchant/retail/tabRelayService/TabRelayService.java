package com.paypal.merchant.retail.tabRelayService;

import com.paypal.merchant.retail.log4jLogger.LogManager;
import com.paypal.merchant.retail.sdk.contract.PayPalMerchantRetailSDK;
import com.paypal.merchant.retail.sdk.contract.commands.CommandBuilderContext;
import com.paypal.merchant.retail.sdk.contract.commands.GetTabsCommand;
import com.paypal.merchant.retail.sdk.contract.commands.GetTabsRequest;
import com.paypal.merchant.retail.sdk.contract.exceptions.PPConfigurationException;
import com.paypal.merchant.retail.sdk.contract.exceptions.PPInvalidInputException;
import com.paypal.merchant.retail.sdk.internal.commands.PayPalMerchantRetailSDKImpl;
import com.paypal.merchant.retail.sdk.internal.common.ConfigurationImpl;
import com.paypal.merchant.retail.utils.PropertyManager;
import org.apache.log4j.Logger;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.paypal.merchant.retail.sdk.internal.common.ConfigurationImpl.Store;

/**
 * Created by Paolo on 6/27/2014.
 */
public class TabRelayService {
    Logger logger = Logger.getLogger(this.getClass());

    private static final long POLLING_INTERVAL = PropertyManager.INSTANCE.getProperty("getTabs.interval.seconds", 60);

    private PayPalMerchantRetailSDKImpl sdkImpl;

    private List<ScheduledExecutorService> scheduledTasks;

    public TabRelayService() {
        try {
            logger.debug("Creating new instance of PayPalMerchantRetailSDK");
            Source sdkConfig = new StreamSource(this.getClass().getClassLoader().getResourceAsStream("Config.xml"));
            sdkImpl = (PayPalMerchantRetailSDKImpl) PayPalMerchantRetailSDK.newInstance(sdkConfig);
            sdkImpl.registerLogManager(LogManager.newInstance());
            scheduledTasks = new ArrayList<ScheduledExecutorService>();
        } catch (PPConfigurationException e) {
            logger.error("Exception creating new instance of PayPalMerchantRetailSDK ", e);
        } catch (PPInvalidInputException e) {
            logger.error("Exception creating new instance of PayPalMerchantRetailSDK ", e);
        }


    }

    public void restart() {
        stop();
        start();
    }

    public void start() {
        logger.info("Start polling the SDK Service...");
        try{
            logger.debug("Setting the schedule...");

            // For each configured store, start a thread to call getTabs
            for(Store store : sdkImpl.getSdkConfig().getStoreConfigMap().values()) {
                logger.debug("Adding runnable for store: " + store.getId());
                CommandBuilderContext builderContext = CommandBuilderContext.newInstance();
                builderContext.setStoreId(store.getId());
                GetTabsCommand command = buildCommand(builderContext, store);
                ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
                service.scheduleAtFixedRate(new PublishTabsRunnable(store, command), 0, POLLING_INTERVAL, TimeUnit.SECONDS);
                scheduledTasks.add(service);
            }

            logger.debug("Finished Setting the schedule...");
        } catch (Exception e) {
            logger.error("Failed to start tabRelayService:", e);
        }
    }

    public void stop() {
        logger.info("Trying to stop polling the SDK Service...");
        if(scheduledTasks != null) {
            for(ScheduledExecutorService service : scheduledTasks) {
                service.shutdown();
            }
        }
    }


    private GetTabsCommand buildCommand(CommandBuilderContext builderContext, ConfigurationImpl.Store store) throws PPInvalidInputException {
        try {
            GetTabsRequest request = GetTabsRequest.newInstance();
            request.setLocationId(store.getLocationId());
            request.setSortType(GetTabsRequest.TabsSortType.NameAscending);
            builderContext.setStoreId(store.getId());
            return sdkImpl.newCommandBuilder(builderContext).build(request);
        } catch (PPInvalidInputException e) {
            logger.error("Exception getting instance of CommandBuilder", e);
            throw e;
        }
    }




}
