
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FTPServer {
    private final static String HOST = "127.0.0.1";
    final private static Path FILEPATH = Paths.get(System.getProperty("user.dir"), "PSP-FTP", "files");

    public static void main(String[] args) throws IOException {

        FTPClient cliente = new FTPClient();
        cliente.connect(HOST);

        if (FTPReply.isPositiveCompletion(cliente.getReplyCode()))
            System.out.println("(" + cliente.getReplyCode() + ").Reply Code Possitive. Host Reached.");
        else {
            System.out.println("(" + cliente.getReplyCode() + ").Connection error.");
        }

        cliente.login("brunova", "kaos");
        System.out.printf("(%d).Login correcto.%n", cliente.getReplyCode());

        cliente.enterLocalPassiveMode();
        System.out.printf("(%d).Modo pasivo activado.%n", cliente.getReplyCode());

        System.out.printf("(%d).Ubicacion actual: %s %n", cliente.getReplyCode(), cliente.printWorkingDirectory());

        BorrarDirectorios(cliente);

        //Crea un directorio con nombre YYYYMMDDHHMMSS en la raíz.
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        cliente.makeDirectory(cliente.printWorkingDirectory() + date);

        //Mueve los ficheros de la raiz a la carpeta recién creada.
        FTPFile[] files = cliente.listFiles();
        for (FTPFile file : files) {
            if (file.isFile())
                System.out.printf("Moviendo: [%s] a [%s]%n", cliente.printWorkingDirectory() + file.getName(), "/" + date + "/" + file.getName());
            cliente.rename(cliente.printWorkingDirectory() + "/" + file.getName(), "/" + date + "/" + file.getName());
        }

        //Sube los archivos de la carpeta files a la raiz.
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(FILEPATH)) {
            for (Path file : directoryStream) {
                System.out.println(file);
                try (FileInputStream fis = new FileInputStream(file.toString())) {
                    cliente.storeFile(file.getFileName().toString(), fis);
                } catch (IOError e) {
                    System.err.println("Error al subir el archivo.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error al buscar archivos en el directorio.");
        }

        cliente.logout();
    }

    static void ListadoRecursivo(FTPClient cliente) throws IOException {
        FTPFile[] files = cliente.listFiles();
/*        if (files.length == 0)
            System.out.println("No files.");*/
        for (FTPFile file : files) {
            System.out.printf("%s %s %n", file.getName(), file.isDirectory() ? "[Directorio]" : "[Fichero]");
            if (file.isDirectory()) {
                cliente.changeWorkingDirectory(cliente.printWorkingDirectory() + "/" + file.getName());
                ListadoRecursivo(cliente);
                cliente.changeToParentDirectory();
            }
        }
    }

    //Éste método permite borrar recursivamente los directorios conservando los ficheros de la raíz.
    private static void BorrarDirectorios(FTPClient cliente) throws IOException {
        FTPFile[] files = cliente.listFiles();
        for (FTPFile file : files) {
            if (file.isDirectory()) {
                cliente.changeWorkingDirectory(cliente.printWorkingDirectory() + "/" + file.getName());
                BorradoRecursivo(cliente);
                cliente.changeToParentDirectory();
                System.out.printf("Borrando fichero: %s %n", file.getName());
                cliente.removeDirectory(file.getName());
            }
        }
    }

    private static void BorradoRecursivo(FTPClient cliente) throws IOException {
        FTPFile[] files = cliente.listFiles();
        for (FTPFile file : files) {
            System.out.printf("Borrando: %s %s %n", file.getName(), file.isDirectory() ? "[Directorio]" : "[Fichero]");
            if (file.isDirectory()) {
                cliente.changeWorkingDirectory(cliente.printWorkingDirectory() + "/" + file.getName());
                BorradoRecursivo(cliente);
                cliente.changeToParentDirectory();
                cliente.removeDirectory(file.getName());
            } else {
                cliente.deleteFile(file.getName());
            }
        }
    }
}
