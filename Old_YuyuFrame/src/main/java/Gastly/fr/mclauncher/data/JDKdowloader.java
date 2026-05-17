package Gastly.fr.mclauncher.data;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JDKdowloader {
    public static void downloadAndExtractGraalVM(String fileURL, String saveDir) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setRequestMethod("GET");

        int responseCode = httpConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
            String saveFilePath = saveDir + File.separator + fileName;

            // Téléchargement
            try (InputStream inputStream = httpConnection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(saveFilePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("GraalVM JDK téléchargé avec succès : " + saveFilePath);

            // Extraction si ZIP
            if (fileName.endsWith(".zip")) {
                extractZip(saveFilePath, saveDir);

                String graalVMPath = saveDir;
                System.setProperty("JAVA_HOME", graalVMPath);
                System.out.println("JAVA_HOME défini sur : " + System.getProperty("JAVA_HOME"));

                try {
                    Process process = Runtime.getRuntime().exec(graalVMPath + "/bin/java -version");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                            if (line.contains("GraalVM")) {
                                System.out.println("GraalVM détecté avec succès.");
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("GraalVM non détecté.");
                }
            }
        } else {
            System.out.println("Échec du téléchargement. Code HTTP : " + responseCode);
        }
        httpConnection.disconnect();
    }

    public static void extractZip(String zipFilePath, String destDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String filePath = destDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zipIn.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                } else {
                    new File(filePath).mkdirs();
                }
                zipIn.closeEntry();
            }
        }
        System.out.println("Extraction terminée.");
    }
}
