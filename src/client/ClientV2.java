package client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by sam on 18/05/17.
 */
public class ClientV2 {

    private String identifiant;
    private String destinataire;
    private List<String> destinataires;
    private SSLSocket socket;
    private InetAddress server;
    private String timestamp;
    private String user;
    private String usermail;
    private String to;
    private int port;
    private int state;/*
        0 : Closed
        1 : Waiting
        2 : Connected
        3 : Sender
        4 : Receiver
        5 : DataWaiting
        6 : Data
    */
    private OutputStream out;
    private BufferedReader in;

    public ClientV2(InetAddress server, int port) {
        this.user = "Larry Skywalker";
        this.usermail = "larryskywalker@hotmail.com";
        this.server = server;
        this.port = port;
        this.state=1;
        try {
            SSLSocketFactory fabrique= (SSLSocketFactory) SSLSocketFactory.getDefault();
            this.socket= (SSLSocket) fabrique. createSocket (server, port);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
//            this.socket = new Socket(server, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.timestamp = "";
        this.destinataires = new ArrayList<>();
    }


    void start() throws IOException {

        in = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            System.out.println("1");
        }
        out = null;
        try {
            out = socket.getOutputStream();
        } catch (IOException ex) {
            System.out.println("2");
        }
        ArrayList<String> strSplitted;
        Scanner sc = new Scanner(System.in);
        String request;
        String response;
        Scanner s;

        //on lit la première réponse du serveur
        response = in.readLine();
        System.out.println(response);
        strSplitted = new ArrayList<>();
        s = new Scanner(response).useDelimiter("\\s+");
        while (s.hasNext()) {
            strSplitted.add(s.next());
        }
        if (strSplitted.get(0).equals("+OK")){
            switch (state){
                case 1:
                    for (int i = 1; i < strSplitted.size() && timestamp.length() < 1; i++) {
                        String str = strSplitted.get(i);
                        if(str.substring(0,1).equals("<")
                                && str.substring(str.length()-1).equals(">")){
                            timestamp = str;
                        }
                    }
                    if(timestamp.length() > 0){
                        //this.state = 2;
                    } else {
                        System.out.println("error : no timestamp received");
                    }
                    break;
                default:
                    break;
            }
        }
        while(!identification());
        destination();
        writeMessage();

        out.write(("QUIT" + "\r\n").getBytes());
        socket.close();
    }

    private boolean identification(){
        System.out.println("Identifiant :");
        Scanner sc = new Scanner(System.in);
        String str = sc.nextLine();
        identifiant=str;
        if (!identifiant.contains("@")) {
            System.out.println("adresse mail invalide");
            return false;
        }else{
            try {
                out.write(("EHLO "+ identifiant.substring(identifiant.indexOf("@")) + "\r\n").getBytes());
                if(getOneLine(in)){
                    System.out.println("MAIL FROM "+ identifiant );
                    out.write(("MAIL FROM "+ identifiant + "\r\n").getBytes());
                    if(getOneLine(in)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
    }}

    private void destination() throws IOException {
        boolean bool = true;
        do{
            String str;
            Scanner sc;
                System.out.println("Entrez un destinataire :");
                sc = new Scanner(System.in);
                str = sc.nextLine();
            if(!testDestinataire(str)){
                System.out.println("ce destinataire n'est pas valide : " + str);
            }else{
                destinataires.add(str);
            }
            System.out.println("Voulez vous ajouter un destinataire : (oui/non)");
            str = sc.nextLine();
            if(!str.equals("oui")) bool = false;
        }while (bool);
    }

    private boolean testDestinataire(String request) throws IOException {
        try {
            out.write(("RCPT TO "+ request + "\r\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getOneLine(in);

    }

    private ArrayList<String> writeMessage() {
        ArrayList<String> message = new ArrayList<>();
        String line;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        line = "Date: " + dateFormat.format(date);
        message.add(line);

        line = "From: " + identifiant;
        message.add(line);

        line = "Subject: ";
        System.out.println("Sujet du message :");
        Scanner sc = new Scanner(System.in);
        String str = sc.nextLine();
        line += str;
        message.add(line);

        line = "To: " + Arrays.toString(destinataires.toArray()) + "\r\n";

        message.add(line);

        System.out.println("Entrez le corps du message :");
        while (!line.equals(".")) {
            str = sc.nextLine();
            line = str;
            message.add(line);
        }
        return message;

    }

    private boolean getOneLine(BufferedReader in) throws IOException {
        String response;
        ArrayList<String> responseSplitted = new ArrayList<>();
        do {
            response = in.readLine();
            System.out.println(response);
            Scanner s = new Scanner(response).useDelimiter("\\s+");
            while (s.hasNext()) {
                responseSplitted.add(s.next());
            }
            if(responseSplitted.size()>0) {
                if (responseSplitted.get(0).contains("250")) {
                    return true;
                }
            } else {
                return false;
            }
        }while (responseSplitted.size()==0);
        return false;
    }
}
