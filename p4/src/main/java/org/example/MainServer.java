package org.example;

public class MainServer {

    public static void main(String[] args) throws Exception{


        for(int i = 0; i < Config.SERVER_LIST.size(); i++) {
            //nur Server auf eigener Ip Starten
            if (Config.OWN_IP.equals(Config.SERVER_LIST.get(i).ip())) {

                int port = Config.SERVER_LIST.get(i).port();
                Server server = new Server(port);
                Thread serverThread = new Thread(() -> server.startServer());
                //serverThread.setDaemon(true); // schließt bei testende
                serverThread.start();
            }
        }
    }
}
