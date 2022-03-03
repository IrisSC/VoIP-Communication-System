import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import javax.sound.sampled.LineUnavailableException;


public class UDPAudioReceive implements Runnable {
    static DatagramSocket receiving_socket;

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public static byte[] decryptBlock(byte[] block, int key){
        ByteBuffer unwrapDecrypt = ByteBuffer.allocate(block.length);

        ByteBuffer cipherText = ByteBuffer.wrap(block);
        for(int j = 0; j < block.length/4; j++) {
            int fourByte = cipherText.getInt();
            fourByte = fourByte ^ key; // XOR decrypt
            unwrapDecrypt.putInt(fourByte);
        }
        return unwrapDecrypt.array();
    }

    public void run(){

        //***************************************************
        //Port to open socket on
        int PORT = 55555;
        //***************************************************

        //***************************************************
        //Open a socket to receive from on port PORT

        //DatagramSocket receiving_socket;
        try{
            receiving_socket = new DatagramSocket(PORT);
        } catch (SocketException e){
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Main loop.

        boolean running = true;

        AudioPlayer player = null;
        try {
            player = new AudioPlayer();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        while (running){

            try{

                byte[] buffer = new byte[514];
                DatagramPacket packet = new DatagramPacket(buffer, 0, 514);

                receiving_socket.receive(packet);
                int i;



                byte[] authKeyArr = new byte[2];
                System.arraycopy(buffer, 0, authKeyArr, 0, 2);
                ByteBuffer authKeyByteBuffer = ByteBuffer.wrap(authKeyArr);


                short authKey = authKeyByteBuffer.getShort();


                if(authKey != 10){
                    System.out.println("Incorrect authentication key!");
                } else {

                    //Isolate the
                    byte[] playBuffer = new byte[512];
                    System.arraycopy(buffer, 2, playBuffer, 0, 512);

                    playBuffer = decryptBlock(playBuffer, 442);
                    player.playBlock(playBuffer);

                }





            } catch (IOException e){
                System.out.println("ERROR: TextReceiver: Some random IO error occured!");
                e.printStackTrace();
            }

        }

        //Close the socket
        player.close();

        receiving_socket.close();
        //***************************************************
    }
}
