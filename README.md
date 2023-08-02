# mpdl
![GitHub](https://img.shields.io/github/license/lukeonuke/mpdl)
![GitHub release (with filter)](https://img.shields.io/github/v/release/lukeonuke/mpdl)
![GitHub all releases](https://img.shields.io/github/downloads/lukeonuke/mpdl/total)
![Discord](https://img.shields.io/discord/790676398602715208)

Open Source curseforge modpack downloader/assembler.

## How to use
There are several ways to use mpdl, the most simple one is to define
all the variables as arguments, as such.

> [!IMPORTANT]  
> It is recommended to place your api key in single quotes, to prevent
> your terminal from interpreting it as an environment variable.

```bash
java -jar mpdl.jar <api key> <modpack-dir>
```

You can also omnit the path if you are running the program in the 
modpack directory.
```bash
java -jar mpdl.jar <api key>
```

### The second way
Your can be stored as a enviroment variable named 'API_KEY' on your 
computer, this way you don't have to remember the api key.

```bash
API_KEY='$2a$10$415125.dasda54z85484gfafe.wadfs' java -jar mpdl.jar
```

# How do I get my api key?
1. Go to https://console.curseforge.com/?#/api-keys.
It will ask you to log in with Google.
2. Press generate key if it didn't already generate a key for you.
3. Copy the key.

# Why did you make this, you could have just used prism/overwolf/insert_launcher_here?
Exactly, I do not like using third party launchers. Plus I hate
overwolf because it's a resource hog. (it also doesn't run on linux)



