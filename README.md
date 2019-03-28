AuthServer
==========
![screenshot](https://github.com/DragonetMC/AuthServer/blob/master/assets/login.jpg)

An offline-mode/extra account system for Bukkit/Spigot servers! 

#### This server *must* run under BungeeCord as a child server. 

# Installation
### Configure AuthServer
- 1. Install UltimateRoles
- 2. Download the jar
- 3. Run `java -jar AuthServer-{version}.jar`, it will quit automatically
- 4. Edit the configuration file `config.properties`, change `lobby-server` to your lobby server
- 5. (optional) Replace the language file `lang.properties`
- 6. Run `java -jar AuthServer-{version}.jar` again
### Configure BungeeCord
- 1. Change default server to AuthServer (in `config.properties``)
- 2. Make sure your `lobby-server` is in defined in BungeeCord's `config.yml`
