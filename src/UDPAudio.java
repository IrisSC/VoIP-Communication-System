import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import javax.sound.sampled.LineUnavailableException;

import uk.ac.uea.cmp.voip.DatagramSocket4;

public class UDPAudio implements Runnable {

    static DatagramSocket4 sending_socket;

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public static byte[] encryptBlock(byte[] block, int key){
        //Encrypted audio block will be stored
        ByteBuffer unwrapEncrypt = ByteBuffer.allocate(block.length);

        ByteBuffer plainText = ByteBuffer.wrap(block);
        for( int j = 0; j < block.length/4; j++){
            int fourByte = plainText.getInt();
            fourByte = fourByte ^ key;
            unwrapEncrypt.putInt(fourByte);
        }
        return unwrapEncrypt.array();
    }

    public void run(){
        //***************************************************
        //Port to send to
        int PORT = 55555;
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
            sending_socket = new DatagramSocket4();
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
                short authenticationKey = 10;
                int encryptionKey = 442;

                //Get audio block from microphone
                byte[] buffer = recorder.getBlock();

                //Set encryption key
                byte[] encryptedBlock = encryptBlock(buffer, encryptionKey);

                //Calculate the checksum to send
                int sum = 0;
                for(byte b : encryptedBlock)
                    sum += b;

                // Add the Checksum, AuthKey and Encrypted Block to the packet respectively
                ByteBuffer VoIPpacket = ByteBuffer.allocate(518);
                VoIPpacket.putInt(sum);
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
