package mypackageclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import com.sun.javacard.apduio.*;

/**
 *
 * @author G6 - JavaCard
 */
public class JavaCardHostClient {

    private Socket sock;
    private OutputStream os;
    private InputStream is;
    private Apdu apdu;
    private CadClientInterface cad;

    public JavaCardHostClient() {
        apdu = new Apdu();
    }

    public void startSimulation() {
        try {
            //socket => simulation
            sock = new Socket("localhost", 9025);
            os = sock.getOutputStream();
            is = sock.getInputStream();
            //Initialize the instance card acceptance device
            cad = CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, is, os);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void powerUp() {
        try {
            if (cad != null) {
                cad.powerUp();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void powerDown() {
        try {
            if (cad != null) {
                cad.powerDown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAPDU(byte[] cmnds) {
        if (cmnds.length > 4 || cmnds.length == 0) {
            System.err.println("inavlid commands");
        } else {
            //set en-tête apdu
            apdu.command = cmnds;
            System.out.println("CLA: " + convertToHexa(cmnds[0]));
            System.out.println("INS: " + convertToHexa(cmnds[1]));
            System.out.println("P1: " + convertToHexa(cmnds[2]));
            System.out.println("P2: " + convertToHexa(cmnds[3]));
        }
    }

    //fonction pour set la longueur des data de l'apdu
    public void setLc(byte ln) {
        apdu.Lc = ln;
        System.out.println("Lc: " + convertToHexa(ln));
    }

    public void setDataIn(byte[] data) {
        if (data.length != apdu.Lc) {
            System.err.println("The number of data in the array are more than expected");
        } else {
            //set les données à envoyer vers l'applet
            apdu.dataIn = data;
            for (int dataIndx = 0; dataIndx < data.length; dataIndx++) {
                System.out.println("dataIn" + dataIndx + ": " + convertToHexa(data[dataIndx]));
            }

        }
    }

    public void setLe(byte ln) {
        //reponse APDU data length prévu
        apdu.Le = ln;
        System.out.println("Le: " + convertToHexa(ln));
    }

    public void sendAPDU() {

        try {
            apdu.setDataIn(apdu.dataIn, apdu.Lc);
            cad.exchangeApdu(apdu);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getDataOut() {

        byte[] dOut = apdu.dataOut;
        for (int dataIndx = 0; dataIndx < dOut.length; dataIndx++) {
            System.out.println("dataOut" + dataIndx + ": " + convertToHexa(dOut[dataIndx]));
        }
        return dOut;

    }

    public byte[] getStatus() {
        byte[] statByte = apdu.getSw1Sw2();
        System.out.println("SW1: " + convertToHexa(statByte[0]));
        System.out.println("SW2: " + convertToHexa(statByte[1]));
        return statByte;
    }


    public String convertToHexa(byte atCode) {
        char hex[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        String str2 = "";
        int num = atCode & 0xff;
        int rem;
        while (num > 0) {
            rem = num % 16;
            str2 = hex[rem] + str2;
            num = num / 16;
        }
        if (str2 != "") {
            return str2;
        } else {
            return "0";
        }
    }
    
    public void closeConnection() {
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
