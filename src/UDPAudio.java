import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import uk.ac.uea.cmp.voip.DatagramSocket3;

import javax.sound.sampled.LineUnavailableException;

public class UDPAudio implements Runnable {

    static DatagramSocket3 sending_socket;

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
        short test = 0;
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
            sending_socket = new DatagramSocket3();
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

                //Set encryption key
                byte[] encryptedBlock = encryptBlock(buffer, encryptionKey, shiftKey);

                ByteBuffer VoIPpacket = ByteBuffer.allocate(516);
                VoIPpacket.putShort(test);
                if(test == 19){
                    test = 0;
                }else {
                    test = (short) (test + (short) 1);
                }
                VoIPpacket.putShort(authenticationKey);
                VoIPpacket.put(encryptedBlock);

                //Make a DatagramPacket from it, with client address and port number
                DatagramPacket packet = new DatagramPacket(VoIPpacket.array(), VoIPpacket.array().length, clientIP, PORT);

                //Send it
                sending_socket.send(packet);

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
