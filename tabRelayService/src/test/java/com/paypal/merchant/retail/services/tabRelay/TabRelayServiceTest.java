package com.paypal.merchant.retail.services.tabRelay;

import com.paypal.merchant.retail.tabRelayService.TabRelayService;
import org.testng.annotations.Test;

public class TabRelayServiceTest {

    @Test
    public void testRestart() throws Exception {

    }

    @Test
    public void testStart() throws Exception {
        TabRelayService service = new TabRelayService();
        service.start();
    }

    @Test
    public void testStop() throws Exception {

    }
}