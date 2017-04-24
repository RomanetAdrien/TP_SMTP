package serveur;

import serveur.users.User;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.SocketException;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;

/**
 * Created by p1303175 on 06/03/2017.
 */
public class Communication implements Runnable{

    private SSLSocket conn_cli;
    private int ID;
    private int state;
    private Timestamp timestamp;

    public Communication(SSLSocket conn_cli, int ID) {
        this.conn_cli = conn_cli;
        this.ID = ID;
        this.state = 1;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    private void start() throws IOException {
        try {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(conn_cli.getInputStream()));
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            OutputStream out = null;
            try {
                out = conn_cli.getOutputStream();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }

            String request = null;
            String clientDomain = null;
            String userName = null;
            ArrayList<String> recipients = new ArrayList<>();
            ArrayList<String> requestSplitted = new ArrayList<>();
            StringBuilder data = new StringBuilder();
            switch (state) {
                case 1: //CLOSED
                    out.write(("220 "+Server.domainName+" Simple Mail Transfer Protocol\r\n").getBytes());
                    state = 2;
                    break;
                default:
                    out.write("500 internal error\r\n".getBytes());
                    break;
            }
            while (state > 1) {
                requestSplitted.clear();
                try {
                    assert in != null;
                    request = in.readLine();
                    if (request != null) {
                        Scanner s = new Scanner(request).useDelimiter("\\s+");
                        while (s.hasNext()) {
                            requestSplitted.add(s.next());
                        }
                        System.out.println(request);
                    } else throw new Exception();
                    if(requestSplitted.get(0).equals("EHLO")){
                        switch (state) {
                            case 2: //READY
                                if(requestSplitted.size() > 1){
                                    clientDomain = requestSplitted.get(1);
                                    out.write(("250 "+Server.domainName+"\r\n").getBytes());
                                    state=3;
                                } else {
                                    out.write("501 client domain missing".getBytes());
                                }
                                break;
                            case 6: //DATA
                                data.append(request).append("\r\n");
                                out.write("250 OK".getBytes());
                                break;
                            default:
                                out.write(("503 invalid request").getBytes());
                                break;
                        }
                    } else if (requestSplitted.get(0).equals("MAIL")){
                        switch (state) {
                            case 3: //NEW MESSAGE
                                if(requestSplitted.size() >= 3 && requestSplitted.get(1).equals("FROM")){
                                    userName = requestSplitted.get(2);
                                    out.write(("250 OK").getBytes());
                                    state = 4;
                                } else {
                                    out.write("501 missing parameters".getBytes());
                                }
                                break;
                            case 6: //DATA
                                data.append(request).append("\r\n");
                                out.write("250 OK".getBytes());
                                break;
                            default:
                                out.write("503 invalid request\r\n".getBytes());
                                break;
                        }
                    } else if (requestSplitted.get(0).equals("RCPT")){
                        switch (state) {
                            case 4: //SENDER
                                if (User.getInstance().isUser((requestSplitted.get(1)))){
                                    recipients.add(requestSplitted.get(1));
                                    out.write("250 OK".getBytes());
                                    state = 5;
                                } else {
                                    out.write("550 No such user here.".getBytes());
                                }
                                break;
                            case 5: //RECEIVER
                                if (User.getInstance().isUser((requestSplitted.get(1)))){
                                    recipients.add(requestSplitted.get(1));
                                    out.write("250 OK".getBytes());
                                } else {
                                    out.write("550 No such user here.".getBytes());
                                }
                                break;
                            case 6: //DATA
                                data.append(request).append("\r\n");
                                out.write("250 OK".getBytes());
                                break;
                            default:
                                out.write(("503 invalid request").getBytes());
                                break;
                        }

                    } else if (requestSplitted.get(0).equals("DATA")){
                        switch (state) {
                            case 5: //RECEIVER
                                out.write(("354 Start Mail Input; end with <CR><LF>.<CR><LF>").getBytes());
                                state = 6;
                                break;
                            case 6: //DATA
                                data.append(request).append("\r\n");
                                out.write("250 OK".getBytes());
                                break;
                            default:
                                out.write(("503 invalid request").getBytes());
                                break;
                        }

                    } else if (requestSplitted.get(0).equals("RSET")){
                        switch (state) {
                            case 4: //SENDER
                            case 5: //RECEIVER
                                userName = null;
                                recipients.clear();
                                state = 3;
                                break;
                            case 6: //DATA
                                data.append(request).append("\r\n");
                                out.write("250 OK".getBytes());
                                break;
                            default:
                                out.write(("503 invalid request").getBytes());
                                break;
                        }

                    } else if (requestSplitted.get(0).equals("QUIT")){
                        switch (state) {
                            case 6: //DATA
                                data.append(request).append("\r\n");
                                out.write("250 OK".getBytes());
                                break;
                            default:
                                state = 1;
                                break;
                        }

                    } else if (requestSplitted.get(0).equals(".")){
                        switch (state) {
                            case 6: //DATA
                                data.append(".\r\n");
                                for (String dest : recipients){
                                    PrintWriter outFile = new PrintWriter("msg/" + dest + ".txt");
                                    outFile.print(data);
                                    outFile.close();
                                }
                                userName = null;
                                recipients.clear();
                                out.write("250 OK".getBytes());
                                state = 3;
                                break;
                            default:
                                out.write(("500 not a request").getBytes());
                                break;
                        }

                    } else {
                        switch (state) {
                            case 6: //DATA
                                data.append(request).append("\r\n");
                                out.write("250 OK".getBytes());
                                break;
                            default:
                                out.write(("500 not a request").getBytes());
                                break;
                        }

                    }
                    /*switch (state) {
                        case 2: //READY

                            break;
                         case 3: //NEW MESSAGE

                            break;
                         case 4: //SENDER

                            break;
                         case 5: //RECEIVER

                            break;
                         case 6: //DATA
                             data.append(request).append("\r\n");
                             out.write("250 OK".getBytes());
                            break;
                        default:
                            out.write(("503 invalid request").getBytes());
                            break;
                    }*/
                } catch (Exception ex) {
                    out.write("-ERR internal error\r\n".getBytes());
                    requestSplitted.add("QUIT");
                }

            }

            out.write(("221 " + Server.domainName +" Service closing transmission channel.\r\n").getBytes());

        } catch (SocketException se) {
            System.out.println("Connection closed : "+se.getMessage());
        }
    }

    @Override
    public void run(){
        try {
            this.start();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
