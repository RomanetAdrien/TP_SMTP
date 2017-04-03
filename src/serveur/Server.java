package serveur;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;


/**
 * Created by p1303175 on 06/03/2017.
 */
public class Server {

    private int portServeur = 10010;
    private InetAddress addr;
    SSLServerSocket ss;
    private int ID = 0;
    public final static String domainName = "machine.example";

    public Server() throws IOException {
        ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();
        this.ss = (SSLServerSocket) ssocketFactory.createServerSocket(portServeur);
        ss.setEnabledCipherSuites(ss.getSupportedCipherSuites());
    }

    public void connexion() throws IOException {
        System.out.println("Server waiting");
        try {
            while(true){
                SSLSocket conn_cli = (SSLSocket)ss.accept();
                Communication com = new Communication(conn_cli, ID++);
                new Thread(com).start();
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

    }
}
