import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;
import uk.ac.uea.cmp.voip.DatagramSocket2;
import uk.ac.uea.cmp.voip.DatagramSocket3;
import uk.ac.uea.cmp.voip.DatagramSocket4;

import javax.sound.sampled.LineUnavailableException;


public class UDPAudioReceive implements Runnable {
    static DatagramSocket3 receiving_socket;

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
        //used for reordering the packets
        short order = 0;
        HashMap<Short, byte[]> saved = new HashMap<Short, byte[]>();
        byte[] previousPacket = new byte[512];
        //***************************************************

        //***************************************************
        //Open a socket to receive from on port PORT

        //DatagramSocket receiving_socket;
        try{
            receiving_socket = new DatagramSocket3(PORT);
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
                //extract test number
                byte[] testNum = new byte[2];
                System.arraycopy(buffer, 0, testNum, 0, 2);
                ByteBuffer testNumByteBuffer = ByteBuffer.wrap(testNum);
                short test = testNumByteBuffer.getShort();
                //System.out.println(test)

                //extract authentication key from the packet
                byte[] authKeyArr = new byte[2];
                System.arraycopy(buffer, 2, authKeyArr, 0, 2);
                ByteBuffer authKeyByteBuffer = ByteBuffer.wrap(authKeyArr);
                short authKey = authKeyByteBuffer.getShort();

                //Check if the authentication key is correct
                if(authKey != 10){
                    System.out.println("Incorrect authentication key!");
                } else {

                    //extract the audio from the packet
                    byte[] playBuffer = new byte[512];
                    System.arraycopy(buffer, 4, playBuffer, 0, 512);

                    //Decrypt the audio and play it
                    playBuffer = decryptBlock(playBuffer, 442);
                    //add the packet to hashmap
                    saved.put(test, playBuffer);
                    //put in buffer
                    if(saved.size() >= 5){
                        //get correct packet
                        byte [] nextPacket = saved.get(order);
                        if(nextPacket != null){
                            player.playBlock(nextPacket);
                            saved.remove(order);
                            previousPacket = nextPacket;
                        }
                        else{
                            // play the previous packet if correct packet not available
                            if(previousPacket != null){
                                player.playBlock(previousPacket);
                            }
                        }
                        if(order == 19){
                            order = 0;
                        }else {
                            order = (short) (order + 1);
                        }
                    }
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
