package mypackage;

import javacard.framework.*;

/**
 *
 * @author G6 - JavaCard
 */
public class JavaCardApplet extends Applet {
    
 final static byte CLA_MONAPPLET =(byte)0xB0;
 
  //========APDU Commands==============//
 
  final static byte VERIFIER = (byte) 0x20;
  final static byte CREDIT = (byte) 0x30;
  final static byte DEBIT = (byte) 0x40;
  final static byte GET_BALANCE = (byte) 0x50;
  final static byte UNBLOCK = (byte) 0x60;
  
  //=======Restrictions================//
  
  final static short MAX_BALANCE = 0x7FFF;   // balance max (32767)
  final static short MAX_TRANSACTION_AMOUNT = 0xFF;  // transaction max(255)
  final static byte PIN_TRY_LIMIT =(byte)0x03;  // nb de tentatives pour PIN (current : 3)
  final static byte MAX_PIN_SIZE =(byte)0x08;  // taille max PIN(current: 8)
  
  //signal d'erreurs selon le retour de réponse APDU SW1 et SW2
  final static short SW_VERIFICATION_FAILED = 0x6312;  // signal erreur code PIN
  final static short SW_PIN_VERIFICATION_REQUIRED = 0x6311; //signal code PIN non validé
  final static short SW_INVALID_TRANSACTION_AMOUNT = 0x6A83; //signal transaction invalide ( > max transaction || somme inhabituel)
  final static short SW_EXCEED_MAXIMUM_BALANCE = 0x6A84; //signal limite balance
  final static short SW_NEGATIVE_BALANCE = 0x6A85; //signal balance negative ==> somme debit/credit > balance
  
   //=======Variables================//
  
  OwnerPIN pin;
  short balance;
  
  //=======Methods==================//
  
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new JavaCardApplet(bArray, bOffset, bLength);       
    }

    protected JavaCardApplet(byte[] bArray, short bOffset, byte bLength) {

        pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);

        byte [] pinArr = {1,2,3,4};
        pin.update(pinArr, (short) 0, (byte)pinArr.length);
        register();
        
    }

    public boolean select() {
        if ( pin.getTriesRemaining() == 0 ) return false;
        return true;
  }// 
   
   public void deselect() {
        pin.reset();
  }
   
    public void process(APDU apdu) {
        // L'APDU transporte un tableau d'octets (tampon) pour
        // transférer les octets d'en-tête APDU entrants et sortants
        // entre la carte et le CAD
        // À ce stade, seuls les premiers octets d'en-tête
        // [CLA, INS, P1, P2, P3] sont disponibles dans
        // le tampon APDU.
        // L'interface javacard.framework.ISO7816
        // déclare des constantes pour indiquer le décalage de
        // ces octets dans le tampon APDU.
        byte[] buffer = apdu.getBuffer();

         // check apdu select command
        if ((buffer[ISO7816.OFFSET_CLA] == 0) &&
            (buffer[ISO7816.OFFSET_INS] == (byte)(0xA4))) return;

         // verification du CLA
        if (buffer[ISO7816.OFFSET_CLA] != CLA_MONAPPLET)
          ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);

        switch (buffer[ISO7816.OFFSET_INS]) {
          case GET_BALANCE: getBalance(apdu);
            return;
          case DEBIT: debit(apdu);
            return;
          case CREDIT: credit(apdu);
            return;
          case VERIFIER: verifier(apdu);
            return;
          case UNBLOCK: pin.resetAndUnblock();
            return;
          default: ISOException.throwIt (ISO7816.SW_INS_NOT_SUPPORTED);
        }
  } 
    
    private void credit(APDU apdu) {
        //pin authentification
        if ( ! pin.isValidated()) ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        byte[] buffer = apdu.getBuffer();

        //Lc representes le nombre de bytes contenu dans le champ data de la commande apdu
        byte numBytes = buffer[ISO7816.OFFSET_LC];

        //indique que l'apdu va recevoir et envoyer les données sur l'offeset ISO7816.OFFSET_CDATA par les 5 bytes principaux
        byte byteRead = (byte)(apdu.setIncomingAndReceive());

        //renvoie  une erreur si la taille de data bytes donné n'est pas cohérent avec la lc donné
        if (byteRead != 1) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        // get credit value
        byte creditAmount = buffer[ISO7816.OFFSET_CDATA];

        // valider credit value
        if ( ( creditAmount > MAX_TRANSACTION_AMOUNT) || ( creditAmount < 0 ) )
          ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);

        if ( ( balance + creditAmount) > MAX_BALANCE ) ISOException.throwIt(SW_EXCEED_MAXIMUM_BALANCE);

        // creditation de la balance
        balance = (short)(balance + creditAmount);
    
  }
    
    private void debit(APDU apdu) {
   
        // pin authentification
        if ( ! pin.isValidated()) ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);

        byte[] buffer = apdu.getBuffer();
        byte numBytes = (byte)(buffer[ISO7816.OFFSET_LC]);
        byte byteRead = (byte)(apdu.setIncomingAndReceive());

        if (byteRead != 1) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        // get debit value
        byte debitAmount = buffer[ISO7816.OFFSET_CDATA];

        // valider debit value
        if ( ( debitAmount > MAX_TRANSACTION_AMOUNT) || (debitAmount < 0 ) )
          ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);

        if  ( (balance-debitAmount) < 0 ) ISOException.throwIt(SW_NEGATIVE_BALANCE);

        //debitation de la balance
        balance = (short) (balance - debitAmount);
  } 
    
    private void getBalance(APDU apdu) {
        if ( ! pin.isValidated()) ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        byte[] buffer = apdu.getBuffer();
        
        //informe le systeme que l'applet a fini de procéder à la commande apdu 
        //et va passer a renvoyer la reponse APDU qui contiennent les données
        short le = apdu.setOutgoing();

        if ( le < 2 ) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        //set bytes retournés en information le cad
        apdu.setOutgoingLength((byte)2);

        // transférer la valeur de la balance dans l'apdu buffer 
        // en commençant par l'offset 0
        buffer[0] = (byte)(balance >> 8);
        buffer[1] = (byte)(balance & 0xFF);

        // envoi des bytes de la balance
        apdu.sendBytes((short)0, (short)2);
  }
    
    private void verifier(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        // retrouver le code pin inséré depuis le coté client
        byte byteRead = (byte)(apdu.setIncomingAndReceive());

        // Les données du code PIN sont lues dans le tampon APDU
        // à l'offset ISO7816.OFFSET_CDATA
        // La longueur des données du code PIN = byteRead
        if ( pin.check(buffer, ISO7816.OFFSET_CDATA,byteRead) == false )
          ISOException.throwIt(SW_VERIFICATION_FAILED);
  } 
}
  