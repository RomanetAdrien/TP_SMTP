package server;

import java.io.IOException;

/**
 * Created by p1303175 on 06/03/2017.
 */
public class Main {

    public static void main(String[] args) throws IOException {
//        ScannerDePorts.scan(0,50000);

        Server server = new Server();
        server.connexion();

    }

}
