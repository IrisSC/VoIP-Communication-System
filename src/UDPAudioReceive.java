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

        short packCount = 1;
        short lastPack = 0;
        byte[] lastPlayBuffer = new byte[512];
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

                byte[] buffer = new byte[516];
                DatagramPacket packet = new DatagramPacket(buffer, 0, 516);

                receiving_socket.receive(packet);
                int i;

                //extract authentication key from the packet
                byte[] authKeyArr = new byte[2];
                System.arraycopy(buffer, 2, authKeyArr, 0, 2);
                ByteBuffer authKeyByteBuffer = ByteBuffer.wrap(authKeyArr);
                short authKey = authKeyByteBuffer.getShort();

                byte[] packNumArr = new byte[2];
                System.arraycopy(buffer,0,packNumArr,0,2);
                ByteBuffer packNumBuffer = ByteBuffer.wrap(packNumArr);
                short packNum = packNumBuffer.getShort();

                //Check if the authentication key is correct
                if(authKey != 10){
                    System.out.println("Incorrect authentication key!");
                }
                else if(packCount - 1 != ((int) lastPack)){
                    player.playBlock(lastPlayBuffer);

                }
                else {
                    packCount++;
                    //extract the audio from the packet
                    System.out.println("Packet Number: " + packNum + " ,Packet Count: " + packCount + " ,Last Packet: " + lastPack);
                    byte[] playBuffer = new byte[512];
                    System.arraycopy(buffer, 4, playBuffer, 0, 512);

                    //Decrypt the audio and play it
                    playBuffer = decryptBlock(playBuffer, 442);
                    player.playBlock(playBuffer);
                    lastPlayBuffer = decryptBlock(playBuffer, 442);
                    lastPack = packNum;
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
