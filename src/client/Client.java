package client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;


public class Client {
    private SSLSocket socket;
    private InetAddress server;
    private String timestamp;
    private String user;
    private String usermail;
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

    public Client(InetAddress server, int port) {
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


        //on lit la première réponse du server
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
        while (state > 0) {
            System.out.println("Veuillez saisir une requète :");
            String str = sc.nextLine();
            request = str;
            if(request.length() > 3) {
                if (request.substring(0, 4).equals("EHLO") || request.substring(0, 4).equals("HELO")) {
                    switch (state) {
                        case 1:
                            out.write((request + "\r\n").getBytes());
                            if (this.getOneLine(in)) {
                                this.state = 2;
                            }
                            break;
                        default:
                            System.out.println("Invalid Request");
                            break;
                    }
                } else if ((request.substring(0, 4).equals("MAIL"))) {
                    switch (state) {
                        case 2:
                            out.write((request + "\r\n").getBytes());
                            if (this.getOneLine(in)) {
                                this.state = 3;
                            }
                            break;
                        default:
                            System.out.println("Invalid Request");
                            break;
                    }
                } else if (request.substring(0, 4).equals("RCPT")) {
                    switch (state) {
                        case 3:
                            out.write((request + "\r\n").getBytes());
                            if (this.getOneLine(in)) {
                                this.state = 4;
                            }
                            break;
                        case 4:
                            out.write((request + "\r\n").getBytes());
                            this.getOneLine(in);
                            break;
                        default:
                            System.out.println("Invalid Request");
                            break;
                    }
                } else if (request.substring(0, 4).equals("RSET")) {
                    switch (state) {
                        default:
                            out.write((request + "\r\n").getBytes());
                            if (this.getOneLine(in)) {
                                this.state = 1;
                            }
                            break;
                    }
                } else if (request.substring(0, 4).equals("DATA")) {
                    switch (state) {
                        default:
                            out.write((request + "\r\n").getBytes());
                            String response2 = in.readLine();
                            System.out.println(response2);
                            if (response2.contains("354")) {
                                for(String msg : writeMessage()){
                                    out.write((msg+"\r\n").getBytes());
                                }
                            } if (this.getOneLine(in)) {
                                this.state = 2;
                            }
                            break;
                    }
                } else if (request.substring(0, 4).equals("QUIT")) {
                    out.write((request + "\r\n").getBytes());
                    if (this.getOneLine(in)) {
                        this.state = 0;
                    }
                    break;
                } else {
                    System.out.println("Invalid Request");
                }
            }else {
                System.out.println("Invalid Request");
            }
        }
        socket.close();

    }

    private ArrayList<String> writeMessage() {
        ArrayList<String> message = new ArrayList<>();
        String line;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        line = "Date: " + dateFormat.format(date);
        message.add(line);

        line = "From: " + user + " <" + usermail + ">";
        message.add(line);

        line = "Subject: ";
        System.out.println("Sujet du message :");
        Scanner sc = new Scanner(System.in);
        String str = sc.nextLine();
        line += str;
        message.add(line);

        line = "To: ";

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

