import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import uk.ac.uea.cmp.voip.DatagramSocket2;

import javax.sound.sampled.LineUnavailableException;

public class UDPAudio implements Runnable {

    static DatagramSocket2 sending_socket;

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public static byte[] encryptBlock(byte[] block, int key, int shiftKey){
        //Encrypted audio block will be stored
        ByteBuffer unwrapEncrypt = ByteBuffer.allocate(block.length);

        ByteBuffer plainText = ByteBuffer.wrap(block);
        for( int j = 0; j < block.length/4; j++){
            int fourByte = plainText.getInt();
            fourByte = fourByte ^ key;
            unwrapEncrypt.putInt(fourByte);
        }

        byte [] firstSection = Arrays.copyOfRange(unwrapEncrypt.array(), 0 , shiftKey);
        byte [] secondSection = Arrays.copyOfRange(unwrapEncrypt.array(), shiftKey, unwrapEncrypt.array().length);
        byte [] shiftArray = new byte [unwrapEncrypt.array().length];
        System.arraycopy(secondSection, 0, shiftArray, 0, secondSection.length);
        System.arraycopy(firstSection, 0, shiftArray, secondSection.length, firstSection.length);
        return shiftArray;
    }

    public void run(){
        //***************************************************
        //Port to send to
        int PORT = 55555;
        //encryption
        short authenticationKey = 10;
        int encryptionKey = 255;
        int shiftKey = 10;
        //Packet number tracking
        short packetNum = 0;
        ByteBuffer [][] interleave = new ByteBuffer[4][4];
        int rows = 0;
        int columns = 0;
        //adding packet to previous one
        byte [] previousPack = new byte[512];
        //IP ADDRESS to send to
        InetAddress clientIP = null;
        try {
            clientIP = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            System.out.println("ERROR: TextSender: Could not find client IP");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Open a socket to send from
        //We dont need to know its port number as we never send anything to it.
        //We need the try and catch block to make sure no errors occur.

        //DatagramSocket sending_socket;
        try{
            sending_socket = new DatagramSocket2();
        } catch (SocketException e){
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Get a handle to the Standard Input (console) so we can read user input

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        //***************************************************

        //***************************************************
        //Main loop.

        boolean running = true;
        AudioRecorder recorder = null;
        try {
            recorder = new AudioRecorder();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        while (running){
            try{
                //Get audio block from microphone
                byte[] buffer = recorder.getBlock();

                //check to make sure this isn't the first packet
                if(previousPack != null){
                    //Encrypt buffer
                    byte[] encryptedBlock2 = encryptBlock(buffer, encryptionKey, shiftKey);
                    //Encrypt previous buffer
                    byte[] encryptedBlock1 = encryptBlock(previousPack, encryptionKey, shiftKey);

                    ByteBuffer VoIPpacket = ByteBuffer.allocate(1028);
                    VoIPpacket.putShort(packetNum);
                    VoIPpacket.putShort(authenticationKey);
                    VoIPpacket.put(encryptedBlock1);
                    VoIPpacket.put(encryptedBlock2);
                    packetNum = (short) (packetNum + 1);
                    if(packetNum == 32){
                        packetNum = 0;
                    }

                    interleave[rows][columns] = VoIPpacket;
                    if(columns == 3 && rows == 3){
                        for(int i=0; i<4; i++){
                            for(int j=0; j<4; j++){
                                ByteBuffer sendPacket = interleave[i][j];
                                //Make a DatagramPacket from it, with client address and port number
                                DatagramPacket packet = new DatagramPacket(sendPacket.array(), sendPacket.array().length, clientIP, PORT);
                                //Send it
                                sending_socket.send(packet);
                            }
                        }
                    }
                    rows = rows + 1;
                    if(rows == 4){
                        rows = 0;
                        columns = columns + 1;
                    }
                    if (columns ==4) {
                        columns = 0;
                    }
                }
                //copy buffer to be previous packet
                System.arraycopy(buffer, 0,previousPack, 0, 512);
            } catch (IOException e){
                System.out.println("ERROR: TextSender: Some random IO error occured!");
                e.printStackTrace();
            }
        }
        //Close the socket
        recorder.close();
        sending_socket.close();
        //***************************************************
    }
}
