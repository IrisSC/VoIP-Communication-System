public class AudioDuplex {
    public static void main (String[] args){
        //testing commit
        UDPAudioReceive receiver = new UDPAudioReceive();
        UDPAudio sender = new UDPAudio();

        receiver.start();
        sender.start();
    }
}
