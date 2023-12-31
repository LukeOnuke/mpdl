package com.lukeonuke;

import com.google.gson.Gson;
import com.lukeonuke.model.CFDownloadFile;
import com.lukeonuke.model.CFFile;
import com.lukeonuke.model.CFProject;
import com.lukeonuke.model.Manifest;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Objects;

import static java.lang.System.*;

public class Main {
    private static final DecimalFormat df = new DecimalFormat("0.00");
    public static String[] ARGUMENTS;
    public static String API_KEY;

    public static void main(String[] args) {
        ARGUMENTS = args;

        //if (args.length == 0) help();
        if (args.length > 0 && Objects.equals(args[0], "help")) help();

//        if (args.length < 2) {
//            err.println("ERROR: No modpack directory provided or api key.");
//            err.println("--------");
//            err.println("Provide modpack directory as the first argument of this application.");
//            err.println("Provide API key as the second argument of this application.");
//            err.println("--------");
//            err.println("ex: java -jar mpdl.jar \"c://Users/me/Downloads/modpack\" '$2a$10$xG3FcKtxFaELSwuZtBUideqKkLnTn9ODNp0GvIBBST/o/Umm4cyz2'");
//            exit(1);
//        }

        if (args.length > 0) API_KEY = args[0];
        // Read api key from env
        if (!Objects.isNull(getenv("API_KEY"))) API_KEY = getenv("API_KEY");
        assertNotNull(API_KEY, "API key");


        Path defaultPath = Path.of("");
        Path modpackFolder = defaultPath;
        // Set first argument as modpack folder if there are arguments.
        if(args.length > 0){
            modpackFolder = Path.of(args[0]);
        }
        // Check if second argument is a file, and if yes set it as the modpack folder.
        if (args.length > 1 && !modpackFolder.toFile().exists()) {
            modpackFolder = Path.of(args[1]);
        }
        // Return to default if none are files.
        if(!modpackFolder.toFile().exists()){
            modpackFolder = defaultPath;
        }
        modpackFolder = modpackFolder.toAbsolutePath();
        assertNotNull(modpackFolder, "Modpack folder");


        out.println("Modpack folder is " + modpackFolder);
        out.println("Token is " + API_KEY);
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

                        if (mod.data.downloadUrl == null) {
                            undownloadableFiles.add(cfFile);
                            out.println(Color.RED + "Download url is null, adding to list." + Color.RESET);
                            break;
                        }

                        out.println("Got download url " + Color.BLUE + mod.data.downloadUrl + Color.RESET);
                        // Download file
                        InputStream in = new URL(mod.data.downloadUrl).openStream();
                        Files.copy(in, modsFolder.resolve(mod.data.fileName), StandardCopyOption.REPLACE_EXISTING);
                        in.close();

                        out.println(Color.GREEN_BG + "Got " + mod.data.fileName +
                                " ( " + df.format(mod.data.fileLength / 1000F) + "KB )" + Color.RESET);
                        break;
                    } catch (IOException e) {
                        out.println(Color.RED_BG + "Failed to get fileId=" + Color.GREEN + cfFile.fileID + Color.RESET + Color.RED_BG + " modId=" + Color.BLUE + cfFile.projectID + Color.RESET);
                        faults++;

                        // Fail
                        if (faults < 4) {
                            out.println("Retrying in 15 seconds...");
                        } else {
                            err.println("Failed mod download, exiting.");
                            System.exit(1);
                        }

                        try {
                            Thread.sleep(15000);
                        } catch (InterruptedException ex) {
                            writeError(ex);
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
                    writeError(e);
                }
            });
        } catch (IOException e) {
            writeError(e);
        }

    }

    public static String getUrl(String url) throws IOException {
//        ProcessBuilder builder = new ProcessBuilder();
//        builder.command(
//                "script", "-c",
//                "wget -q -O - " +
//                        "--header='Accept: application/json' " +
//                        "--header='x-api-key: " + ARGUMENTS[1] + "' " +
//                        "--header='User-Agent: Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36' " +
//                        url,
//                "-f",
//                "-q"
//        );
//        Process wget = builder.start();
//        String stringReturn = new String(wget.getInputStream().readAllBytes());
//
//        wget.destroy();
//        return stringReturn;

        URL urlObject = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) urlObject.openConnection();
        con.setRequestMethod("GET");

        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("x-api-key", API_KEY);
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36");

        con.connect();
        if (con.getResponseCode() != 200)
            throw new IOException("RESPONSE CODE NOT HTTP/OK, actual value " + con.getResponseCode() + " " + con.getResponseMessage());

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        return content.toString();
    }

    public static void writeError(Exception ex){
        err.println();
        err.println("For help use help as the first argument. ex: mpdl help");
        err.println();
        err.println("||=====================||");
        err.println("|| UNRECOVERABLE ERROR ||");
        err.println("||=====================||");
        err.println();
        err.println(ex.getClass().getSimpleName() + " : " + ex.getMessage());
        err.println();
        err.println("Stacktrace : ");
        ex.printStackTrace(err);
        System.exit(1);
    }

    public static void assertNotNull(Object obj, String name){
        if(obj == null) writeError(new InputMismatchException(name + " can't be blank (NULL)."));
    }

    public static void help(){
        out.println("MPDL v1.0.0 - Commandline curseforge modpack downloader/assembler");
        out.println("by lukeonuke (https://github.com/LukeOnuke)");
        out.println("================================================================================");
        out.println("Licenced with the MIT licence, to view it visit");
        out.println("https://github.com/LukeOnuke/mpdl/blob/main/LICENSE .");
        out.println();
        out.println("Usage : mpdl [curseforge-api-key] [modpack-dir]");
        out.println("ex: java -jar mpdl.jar '$2a$10$xG3FcKtxFaELSwuZtBUideqKkLnTn9ODNp0GvIBBST/o/Umm4cyz2' \"c://Users/me/Downloads/modpack\"");
        out.println();
        out.println("To skip declaring the api key in the arguments, set the environment variable");
        out.println("API_KEY to your api key.");
        out.println();
        out.println("This program uses ANSI escape codes for color and formatting, so if you are");
        out.println("on windows run it in powershell.");
        exit(0);
    }
}