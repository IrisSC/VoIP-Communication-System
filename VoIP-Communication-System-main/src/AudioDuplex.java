public class AudioDuplex {
    public static void main (String[] args){

        UDPAudioReceive receiver = new UDPAudioReceive();
        UDPAudio sender = new UDPAudio();

        receiver.start();
        sender.start();
    }
}
