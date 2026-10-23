package me.openliven.kothplate.service;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultEconomyServiceTest {
    @Test
    void missingProviderFailsDepositInsteadOfThrowing() {
        VaultEconomyService service = new VaultEconomyService(() -> null);

        EconomyDepositResult result = service.deposit(null, 10.0);

        assertFalse(result.successful());
        assertTrue(result.errorMessage().contains("economy provider"));
    }

    @Test
    void providerRegisteredAfterCreationIsUsed() {
        AtomicReference<Economy> registered = new AtomicReference<>();
        VaultEconomyService service = new VaultEconomyService(registered::get);

        assertFalse(service.deposit(null, 10.0).successful());

        registered.set(economyReturning(new EconomyResponse(10.0, 10.0, EconomyResponse.ResponseType.SUCCESS, null)));

        assertTrue(service.deposit(null, 10.0).successful());
    }

    @Test
    void providerFailureMessageIsPassedThrough() {
        VaultEconomyService service = new VaultEconomyService(
                () -> economyReturning(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "account locked")));

        EconomyDepositResult result = service.deposit(null, 10.0);

        assertFalse(result.successful());
        assertEquals("account locked", result.errorMessage());
    }

    private static Economy economyReturning(EconomyResponse response) {
        return (Economy) Proxy.newProxyInstance(
                Economy.class.getClassLoader(),
                new Class<?>[]{Economy.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("depositPlayer")) {
                        return response;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
