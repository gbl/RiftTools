/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.rifttools;

/**
 *
 * @author gbl
 */
public interface ModConfigurationHandler {
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event);
    public Configuration getConfig();
}
