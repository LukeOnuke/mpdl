package com.lukeonuke;

import com.google.gson.Gson;
import com.lukeonuke.model.CFDownloadFile;
import com.lukeonuke.model.CFFile;
import com.lukeonuke.model.CFProject;
import com.lukeonuke.model.Manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;

import static java.lang.System.*;

public class Main {
    private static final DecimalFormat df = new DecimalFormat("0.00");
    public static String[] ARGUMENTS;

    public static void main(String[] args) {
        ARGUMENTS = args;
        if (Objects.equals(args[0], "help")) {
            out.println("MPDL v1.0.0 - Commandline curseforge modpack downloader/assembler");
            out.println("by lukeonuke (https://github.com/LukeOnuke)");
            out.println();
            out.println("Usage : mpdl [modpack-dir] [curseforge-api-key]");
            err.println("ex: java -jar mpdl.jar \"c://Users/me/Downloads/modpack\" '$2a$10$xG3FcKtxFaELSwuZtBUideqKkLnTn9ODNp0GvIBBST/o/Umm4cyz2'");
            System.exit(0);
        }

        if (args.length < 2) {
            err.println("ERROR: No modpack directory provided or api key.");
            err.println("--------");
            err.println("Provide modpack directory as the first argument of this application.");
            err.println("Provide API key as the second argument of this application.");
            err.println("--------");
            err.println("ex: java -jar mpdl.jar \"c://Users/me/Downloads/modpack\" '$2a$10$xG3FcKtxFaELSwuZtBUideqKkLnTn9ODNp0GvIBBST/o/Umm4cyz2'");
            exit(1);
        }


        final Path modpackFolder = Path.of(args[0]);

        out.println("Modpack folder is " + modpackFolder);
        out.println("Token is " + args[1]);
        final Path manifestPath = modpackFolder.resolve("manifest.json");

        out.println("Loading manifest.json from " + manifestPath);

        out.println();

        try {
            Gson gson = new Gson();
            Manifest manifest = gson.fromJson(Files.readString(manifestPath), Manifest.class);

            final Path overridesFolder = modpackFolder.resolve(manifest.overrides);
            final Path modsFolder = overridesFolder.resolve("mods");
            Files.createDirectories(modsFolder);

            out.println(manifest.name);
            out.println(manifest.version);
            out.println(manifest.author);

            ArrayList<CFFile> undownloadableFiles = new ArrayList<>();

            manifest.files.forEach(cfFile -> {
                int faults = 0;
                while (true) {
                    try {
                        out.println("Getting file fileId=" + Color.GREEN + cfFile.fileID + Color.RESET + " modId=" + Color.BLUE + cfFile.projectID + Color.RESET);

                        CFDownloadFile mod = gson.fromJson(getUrl("https://api.curseforge.com/v1/mods/" + cfFile.projectID + "/files/" + cfFile.fileID), CFDownloadFile.class);
                        //Download file
                        if (mod.data.downloadUrl == null) {
                            undownloadableFiles.add(cfFile);
                            out.println(Color.RED + "Download url is null, adding to list." + Color.RESET);
                            break;
                        }

                        out.println("Got download url " + Color.BLUE + mod.data.downloadUrl + Color.RESET);
                        InputStream in = new URL(mod.data.downloadUrl).openStream();
                        Files.copy(in, modsFolder.resolve(mod.data.fileName), StandardCopyOption.REPLACE_EXISTING);
                        in.close();

                        out.println(Color.GREEN_BG + "Got " + mod.data.fileName +
                                " ( " + df.format(mod.data.fileLength / 1000F) + "KB )" + Color.RESET);
                        break;
                    } catch (IOException e) {
                        out.println(Color.RED_BG + "Failed to get fileId=" + Color.GREEN + cfFile.fileID + Color.RESET + Color.RED_BG + " modId=" + Color.BLUE + cfFile.projectID + Color.RESET);
                        e.printStackTrace();
                        faults++;

                        // Fail
                        if (faults < 4) {
                            out.println("Retrying in 15 seconds...");
                        } else {
                            err.println("Failed mod download, exiting");
                            System.exit(1);
                        }

                        try {
                            Thread.sleep(15000);
                        } catch (InterruptedException ex) {
                            err.println("Failed thread sleep, exiting.");
                            System.exit(1);
                        }
                    }
                }
            });

            // Rename overrides to modpack
            if (overridesFolder.toFile().renameTo(modpackFolder.resolve("modpack").toFile()))
                out.println("Renamed overrides to modpack");

            // Report
            out.println("------------------------");
            out.println(Color.GREEN_BG + "Downloaded and formatted modpack successfully" + Color.RESET);
            out.println();
            out.println("Modpack name " + manifest.name);
            out.println("Modpack version " + manifest.version);
            out.println("Minecraft version " + manifest.minecraft.version);
            out.println("Supported modloaders");
            manifest.minecraft.modLoaders.forEach(cfModLoader -> {
                out.println("\t- " + cfModLoader.id + "  " + (cfModLoader.primary ? Color.BLUE + "Primary" + Color.RESET : ""));
            });
            out.println();
            out.println("----========----");
            undownloadableFiles.forEach(undownloadableFile -> {
                try {
                    out.println();
                    out.println("-----");
                    out.println("File fileId=" + Color.GREEN + undownloadableFile.fileID + Color.RESET + " modId=" + Color.BLUE + undownloadableFile.projectID + Color.RESET);
                    CFProject project = gson.fromJson(getUrl("https://api.curseforge.com/v1/mods/" + undownloadableFile.projectID), CFProject.class);
                    out.println(Color.BLUE + project.data.slug + Color.RESET);
                    out.println("https://www.curseforge.com/minecraft/mc-mods/" + project.data.slug + "/files/" + undownloadableFile.fileID);
                    out.println("\t- web    " + project.data.links.websiteUrl);
                    out.println("\t- issues " + project.data.links.issuesUrl);
                    out.println("\t- source " + project.data.links.sourceUrl);
                    out.println("\t- wiki   " + project.data.links.wikiUrl);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static String getUrl(String url) throws IOException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(
                "script", "-c",
                "wget -q -O - " +
                        "--header='Accept: application/json' " +
                        "--header='x-api-key: " + ARGUMENTS[1] + "' " +
                        "--header='User-Agent: Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36' " +
                        url,
                "-f",
                "-q"
        );
        Process wget = builder.start();
        String stringReturn = new String(wget.getInputStream().readAllBytes());

        wget.destroy();
        return stringReturn;
    }
}