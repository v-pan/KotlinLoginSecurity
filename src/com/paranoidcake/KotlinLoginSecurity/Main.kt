package com.paranoidcake.KotlinLoginSecurity

import com.paranoidcake.KotlinLoginSecurity.commands.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.HashMap
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import kotlin.collections.ArrayList

class Main: JavaPlugin() {
    companion object {
        val loggedPlayers = HashMap<String, Boolean>()
        val jailedPlayers = HashMap<UUID, Boolean>()
//        val inventories = HashMap<String, Array<ItemStack>>() TODO: See PlayerHandler.kt

        lateinit var passwords: HashMap<String, String>
        lateinit var jails: ArrayList<Location>

        lateinit var playerPosFile: File
        lateinit var playerPosData: YamlConfiguration

        lateinit var inventoryFile: File
        lateinit var inventoryData: YamlConfiguration

        lateinit var passwordFile: File
        lateinit var passwordData: YamlConfiguration

        val attempts = HashMap<UUID, Int>()

        lateinit var pluginRef: Plugin

        fun broadcast(message: String, color: Any = ChatColor.WHITE) {
            Bukkit.broadcastMessage("[KotLogin]: $color$message")
        }

        fun log(message: String) {

            Bukkit.getLogger().info("[KotlinLoginSecurity] $message")
        }
        fun error(message: String) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "[KotlinLoginSecurity] &4Error: $message"))
        }
    }

    private val jailsFile = File(dataFolder, "jails.yml")
    private val jailsData = YamlConfiguration.loadConfiguration(jailsFile)

    private val loggedPlayersFile = File(dataFolder, "loggedPlayers.yml")
    private val loggedPlayersData = YamlConfiguration.loadConfiguration(loggedPlayersFile)

    override fun onEnable() {
        pluginRef = server.pluginManager.getPlugin("KotlinLoginSecurity")!!

        try {
            val preloadLogged = loggedPlayersData["loggedPlayers"] as ArrayList<String>

            preloadLogged.forEach {
                loggedPlayers[it] = true
            }

            loggedPlayersData["loggedPlayers"] = null
            loggedPlayersData.save(loggedPlayersFile)
        } catch (e: KotlinNullPointerException) {
            log("No players to log in!")
        } catch (e: TypeCastException) {
            log("No players to log in!")
        }

        // --------------------- Assign lateinit vars
        playerPosFile = File(dataFolder, "playerPositions.yml")
        playerPosData = YamlConfiguration.loadConfiguration(playerPosFile)

        inventoryFile = File(dataFolder, "playerInventories.yml")
        inventoryData = YamlConfiguration.loadConfiguration(inventoryFile)

        passwordFile = File(dataFolder, "passwords.yml")
        passwordData = YamlConfiguration.loadConfiguration(passwordFile)

        passwords = try {
            passwordData.getConfigurationSection("passwords")!!.getValues(false) as HashMap<String, String>
        } catch (e: KotlinNullPointerException) {
            HashMap()
        } catch (e: TypeCastException) {
            HashMap()
        }

        try {
            val size = jailsData.get("jails.size").toString().toInt()

            jails = ArrayList()

            for(i in 0 until size) {
                jails.add(Location.deserialize(jailsData.getConfigurationSection("jails.$i")!!.getValues(false)))
            }
        } catch (e: KotlinNullPointerException) {
            broadcast("Null jail saved, generating default jail...", ChatColor.YELLOW)
            val world = pluginRef.server.worlds[0]
            val spawn = world.spawnLocation
            jails = arrayListOf(Location(pluginRef.server.worlds[0], spawn.x, spawn.y, spawn.z))
        } catch (e: NumberFormatException) {
            broadcast("No jails saved, generating default jail...", ChatColor.YELLOW)
            val world = pluginRef.server.worlds[0]
            val spawn = world.spawnLocation
            jails = arrayListOf(Location(pluginRef.server.worlds[0], spawn.x, spawn.y, spawn.z))
        }

        // --------------------- Register events + commands
        server.pluginManager.registerEvents(PlayerListener(), this)

        this.getCommand("register")!!.setExecutor(CommandRegister())
//        this.getCommand("unregister")!!.setExecutor(CommandUnregister()) TODO: Handle hot registration
        this.getCommand("reregister")!!.setExecutor(CommandReregister())
        this.getCommand("login")!!.setExecutor(CommandLogin())
        this.getCommand("listjails")!!.setExecutor(CommandListJails())
        this.getCommand("addjail")!!.setExecutor(CommandAddJail())
        this.getCommand("removejail")!!.setExecutor(CommandRemoveJail())
    }

    override fun onDisable() {
        passwordData.createSection("passwords", passwords)
        passwordData.save(passwordFile)
        log("Passwords saved!")

        val loggedToSave = Array(loggedPlayers.size) { "" }
        var playerWasLogged = false
        var i = 0; loggedPlayers.forEach {
            if(it.value) {
                loggedToSave[i] = it.key
            }
            i++
        }
        for(player in loggedToSave) {
            if (player != "") {
                loggedPlayersData.set("loggedPlayers", loggedToSave)
                loggedPlayersData.save(loggedPlayersFile)
                playerWasLogged = true
                log("Logged in players saved!")

                break
            }
        }

        jailsData.set("jails.size", jails.size)
        for(i in 0 until jails.size) {
            jailsData.createSection("jails.$i", jails[i].serialize())
        }
        jailsData.save(jailsFile)
    }
}