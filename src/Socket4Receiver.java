import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import uk.ac.uea.cmp.voip.DatagramSocket4;

import javax.sound.sampled.LineUnavailableException;


public class Socket4Receiver implements Runnable {
    static DatagramSocket4 receiving_socket;

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
            receiving_socket = new DatagramSocket4(PORT);
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

        byte[] prevPlayBuffer = new byte[512];

        while (running){

            try{

                byte[] buffer = new byte[518];
                DatagramPacket packet = new DatagramPacket(buffer, 0, 518);

                receiving_socket.receive(packet);

                //extract the checksum from the packet
                byte[] checksumReceiveArr = new byte[4];
                System.arraycopy(buffer, 0, checksumReceiveArr, 0, 4);
                ByteBuffer checksumByteBuffer = ByteBuffer.wrap(checksumReceiveArr);
                int sumReceive = checksumByteBuffer.getInt();

                //extract authentication key from the packet
                byte[] authKeyArr = new byte[2];
                System.arraycopy(buffer, 4, authKeyArr, 0, 2);
                ByteBuffer authKeyByteBuffer = ByteBuffer.wrap(authKeyArr);
                short authKey = authKeyByteBuffer.getShort();

                //extract the audio from the packet
                byte[] playBufferEncrypted = new byte[512];
                System.arraycopy(buffer, 6, playBufferEncrypted, 0, 512);
                //Decrypt the audio and play it
                byte[] playBuffer = decryptBlock(playBufferEncrypted, 442);

                // Find the sum of the bytes received
                int sum = 0;
                for(byte b : playBufferEncrypted)
                    sum += b;

                // If the checksum does not match the one sent, play previous block
                if(sumReceive != sum){
                    System.out.println("Incorrect checksum!");
                    player.playBlock(prevPlayBuffer);
                    // If authentication key is incorrect, play previous block
                } else if (authKey != 10){
                    System.out.println("Incorrect authentication key!");
                    player.playBlock(prevPlayBuffer);
                    // Otherwise, just play the current buffer
                } else {
                    player.playBlock(playBuffer);
                    prevPlayBuffer = playBuffer;
                }


            } catch (IOException e){
                System.out.println("ERROR: TextReceiver: Some random IO error occurred!");
                e.printStackTrace();
            }

        }

        //Close the socket
        player.close();

        receiving_socket.close();
        //***************************************************
    }
}
