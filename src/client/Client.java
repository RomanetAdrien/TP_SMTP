package client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Scanner;


public class Client {
    private SSLSocket socket;
    private InetAddress server;
    private String timestamp;
    private int port;
    private int state;

    public Client(InetAddress server, int port) {
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
    }

    void start() throws IOException {

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            System.out.println("1");
        }
        OutputStream out = null;
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
                        this.state = 2;
                    } else {
                        System.out.println("error : no timestamp received");
                    }
                    break;
                default:
                    break;
            }
        }
        while (state > 0){
            System.out.println("Veuillez saisir une requète :");
            String str = sc.nextLine();
            request = str;

            if (request.substring(0, 4).equals("APOP")) {
                switch (state) {
                    case 2:
                        strSplitted = new ArrayList<>();
                        s = new Scanner(request).useDelimiter("\\s+");
                        while (s.hasNext()) {
                            strSplitted.add(s.next());
                        }
                        try {
                            if (strSplitted.size() > 2) {
                                out.write((strSplitted.get(0) + " " + strSplitted.get(1) + " ").getBytes());
                                out.write(this.getAPOPMD5(strSplitted.get(2)).getBytes());
                                out.write(("\r\n").getBytes());
                                this.state = 3;
                                this.getOneLine(in);
                            } else {
                                System.out.println("mot de passe et/ou identifiant manquant");
                            }
                        } catch (NoSuchAlgorithmException e) {
                            System.out.println(e.getMessage());
                        }
                        break;
                    default:
                        System.out.println("Requète non valide");
                        break;
                }
            } else if (request.substring(0, 4).equals("STAT")){
                switch (state) {
                    case 5:
                        out.write((request + "\r\n").getBytes());
                        this.getOneLine(in);
                        break;
                    default:
                        System.out.println("Requête non valide");
                        break;
                }
            }else if ((request.substring(0, 4).equals("LIST"))
                    || (request.substring(0, 4).equals("RETR"))) {
                switch (state) {
                    case 5:
                        out.write((request + "\r\n").getBytes());
                        if(this.getOneLine(in))
                            this.getNextLines(in);
                        break;
                    default:
                        System.out.println("Requête non valide");
                        break;
                }
            } else if (request.substring(0, 4).equals("QUIT")) {
                switch (state) {
                    default:
                        out.write((request + "\r\n").getBytes());
                        this.getOneLine(in);
                        this.state = 0;
                        break;
                }
            }
        }
    }

    private boolean getOneLine(BufferedReader in) throws IOException {
        String response;
        ArrayList<String> responseSplitted = new ArrayList<>();
        do {
            response = in.readLine();
            Scanner s = new Scanner(response).useDelimiter("\\s+");
            while (s.hasNext()) {
                responseSplitted.add(s.next());
            }
            if(responseSplitted.size()>0) {
                if (responseSplitted.get(0).equals("+OK")) {
                    switch (state) {
                        case 3:
                            System.out.println("connecté");
                            this.state = 5;
                            break;
                        default:
                            break;
                    }
                    System.out.println(response);
                    return true;
                } else if (responseSplitted.get(0).equals("-ERR")) {
                    switch (state) {
                        case 3:
                            this.state = 2;
                            break;
                        default:
                            break;
                    }
                    System.out.println(response);
                    return false;
                }
            }
        }while (responseSplitted.size()==0);
        return false;
    }

    private void getNextLines(BufferedReader in) throws IOException {
        String response;
        do {
            response = in.readLine();
            System.out.println(response);
        } while(!response.equals("."));
    }

    private String getAPOPMD5(String pswd) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String str = this.timestamp+pswd;
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] checkSum = md.digest(str.getBytes("UTF-8"));
        return new String(checkSum, "UTF-8").replace("\n","n");
    }
}

