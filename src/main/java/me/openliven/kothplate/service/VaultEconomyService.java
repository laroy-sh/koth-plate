package me.openliven.kothplate.service;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public final class VaultEconomyService implements EconomyService {
    private final Supplier<Economy> economyLookup;

    /**
     * @param economyLookup resolves the current Vault economy provider on every call, so a provider
     *                      registered (or replaced) after this plugin enabled is still picked up.
     */
    public VaultEconomyService(Supplier<Economy> economyLookup) {
        this.economyLookup = economyLookup;
    }

    @Override
    public EconomyDepositResult deposit(Player player, double amount) {
        Economy economy = economyLookup.get();
        if (economy == null) {
            return EconomyDepositResult.failure("no Vault economy provider is registered");
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (response.transactionSuccess()) {
            return EconomyDepositResult.success();
        }
        return EconomyDepositResult.failure(response.errorMessage);
    }
}
