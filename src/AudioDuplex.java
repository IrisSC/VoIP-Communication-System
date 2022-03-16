public class AudioDuplex {
    public static void main (String[] args){

        UDPAudioReceive receiver = new UDPAudioReceive();
        UDPAudio sender = new UDPAudio();

        Socket2Sender sender2 = new Socket2Sender();
        Socket2Receiver receiver2 = new Socket2Receiver();

        Socket3Sender sender3 = new Socket3Sender();
        Socket3Receiver receiver3 = new Socket3Receiver();

        Socket4Sender sender4 = new Socket4Sender();
        Socket4Receiver receiver4 = new Socket4Receiver();
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        receiver.start();
        sender.start();

        //sender2.start();
        //receiver2.start();

        //sender3.start();
        //receiver3.start();

        //sender4.start();
        //receiver4.start();

    }
}
