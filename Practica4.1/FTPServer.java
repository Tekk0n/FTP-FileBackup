
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
        //1.Instancia la clase FTPClient.
        FTPClient cliente = new FTPClient();

        //2.Conecta a local host.
        cliente.connect(HOST);

        //3.Comprueba si la conexión es correcta.
        if (FTPReply.isPositiveCompletion(cliente.getReplyCode()))
            System.out.println("(" + cliente.getReplyCode() + ").Reply Code Possitive. Host Reached.");
        else {
            System.out.println("(" + cliente.getReplyCode() + ").Connection error.");
        }

        //4.Cambia modo pasivo.
        cliente.enterLocalPassiveMode();
        System.out.printf("(%d).Mode: Passive.%n", cliente.getReplyCode());

        //5.Login con el usuario.
        cliente.login("brunova", "kaos");
        System.out.printf("(%d).Correct login.%n", cliente.getReplyCode());

        //6.Imprime el nombre del directorio actual.
        System.out.printf("Current remote folder is %s %n", cliente.printWorkingDirectory());

        //7.Borra todos los directorios que desciendan del directorio actual.
        BorrarDirectorios(cliente);

        //8.Imprime por pantalla todos los ficheros del directorio actual.
        ListarFicheros(cliente);

        //9.Crea un directorio con nombre YYYYMMDDHHMMSS en la raíz.
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        System.out.printf("Creating remote folder: %s %n", date);
        cliente.makeDirectory(cliente.printWorkingDirectory() + date);

        //10.Mueve los ficheros de la raiz a la carpeta recién creada.
        FTPFile[] files = cliente.listFiles();
        for (FTPFile file : files) {
            if (file.isFile())
                System.out.printf("Moving remote file: [%s] TO [%s]%n", cliente.printWorkingDirectory() + file.getName(), "/" + date + "/" + file.getName());
            cliente.rename(cliente.printWorkingDirectory() + "/" + file.getName(), "/" + date + "/" + file.getName());
        }

        //11.Cambia al directorio de la carpeta recién creada.
        cliente.changeWorkingDirectory(cliente.printWorkingDirectory() + date);

        //12.Imprime por pantalla los ficheros en el directorio actual.
        ListadoRecursivo(cliente);

        //13.Cambia al directorio padre e imprime su nombre.
        cliente.changeToParentDirectory();
        System.out.printf("Current remote folder is: %s %n", cliente.printWorkingDirectory());

        //14.Sube al servidor los archivos de la carpeta files (a la raíz).
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(FILEPATH)) {
            for (Path file : directoryStream) {
                try (FileInputStream fis = new FileInputStream(file.toString())) {
                    System.out.printf("Uploading local file: %s %n", file.getFileName());
                    cliente.storeFile(file.getFileName().toString(), fis);
                } catch (IOError e) {
                    System.err.println("Error al subir el archivo.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error al buscar archivos en el directorio.");
        }

        //15.Imprime por pantalla la lista de los ficheros en la raiz.
        ListarFicheros(cliente);

        //16.Haz logout.
        cliente.logout();
    }



    //Éste método permite listar únicamente los ficheros de la raíz (sin directorios, no recursivo)
    //Formato print: -rw-rw-rw- 1 ftp ftp          123382 Dec 03 10:44 The Dunwhich horror by H.P Lovecraft [Fichero]
    static void ListarFicheros(FTPClient cliente) throws IOException {
        System.out.printf("Current remote folder is: %s %n", cliente.printWorkingDirectory());
        FTPFile[] files = cliente.listFiles();
        for (FTPFile file : files) {
            if (file.isFile())
                System.out.printf("%s %s %n", file, file.isDirectory() ? "[Directorio]" : "[Fichero]");
        }
    }

    //Éste método permite listar el contenido del servidor.
    //Formato print: The Dunwhich horror by H.P Lovecraft ([Fichero] | [Directorio])
    static void ListadoRecursivo(FTPClient cliente) throws IOException {
        System.out.printf("Current remote folder is: %s %n", cliente.printWorkingDirectory());
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
                System.out.printf("Deleting remote Folder: %s %n", file.getName());
                cliente.removeDirectory(file.getName());
            }
        }
    }

    private static void BorradoRecursivo(FTPClient cliente) throws IOException {
        FTPFile[] files = cliente.listFiles();
        for (FTPFile file : files) {
            System.out.printf("Deleting remote %s: %s %n", file.isDirectory() ? "Folder" : "File", file.getName());
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
