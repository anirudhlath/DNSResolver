import java.io.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

public class DNSRecord {
    static int padding;
    String[] NAME;
    int TYPE;
    int CLASS;
    int TTL;
    int RDLENGTH;
    byte[] RDATA; // TODO: Byte[]
    Timestamp timestamp;

    public DNSRecord(DataInputStream stream, DNSMessage message) throws IOException {

        /*// Data extracted based on rfc1035
        byte[] buf = stream.readAllBytes();
        stream = new DataInputStream(new ByteArrayInputStream(buf));
        DataInputStream bufstream = new DataInputStream(new ByteArrayInputStream(buf));
        int temp = bufstream.readShort();
        if (DNSServer.debug == 2) {
            System.out.println(Integer.toBinaryString(temp));
            System.out.println(temp & 0b1100000000000000);
            System.out.println(temp & 0b0011111111111111);
        }
        int offset = 0;
        if ((temp & 0b1100000000000000) == 49152) {
            offset = temp & 0b0011111111111111;
            if (DNSServer.debug > 0) {
                System.out.println("<--- COMPRESSION FOUND --->");
                System.out.println("OFFSET:         " + offset);
            }
            NAME = message.readDomainName(offset);
            DNSRecord.padding = stream.readShort(); // I had to add padding for some reason
        } else {
            NAME = message.readDomainName(stream);
        }*/
        NAME = message.readDomainName(stream);
        TYPE = stream.readUnsignedShort();
        CLASS = stream.readUnsignedShort();
        TTL = stream.readInt();
        RDLENGTH = stream.readUnsignedShort();
        RDATA = stream.readNBytes(RDLENGTH);
        timestamp = new Timestamp(System.currentTimeMillis());

        if (DNSServer.debug > 0) {
            System.out.println("<--- DECODED RECORD DATA --->");
            System.out.println("NAME:           " + Arrays.deepToString(NAME));
            System.out.println("TYPE:           " + TYPE);
            System.out.println("CLASS:          " + CLASS);
            System.out.println("TTL:            " + TTL);
            System.out.println("RDLENGTH:       " + RDLENGTH);
            System.out.println("RDATA:          " + Arrays.deepToString(new byte[][]{RDATA}) + "\n");
        }

        // Backup Bytes
        DNSMessage.backupData = stream.readAllBytes(); // Had to backup bytes because Java is the stupidest language.

    }

    public static DNSRecord decodeRecord(ByteArrayInputStream inputStream, DNSMessage message) throws IOException {
        return new DNSRecord(new DataInputStream(inputStream), message);
    }

    void writeBytes(ByteArrayOutputStream stream, HashMap<String, Integer> domainNameLocations) throws IOException {
        if (DNSServer.debug > 0) {
            System.out.println("<--- DECODED RECORD DATA --->");
            System.out.println("NAME:           " + Arrays.deepToString(NAME));
            System.out.println("TYPE:           " + TYPE);
            System.out.println("CLASS:          " + CLASS);
            System.out.println("TTL:            " + TTL);
            System.out.println("RDLENGTH:       " + RDLENGTH);
            System.out.println("RDATA:          " + Arrays.deepToString(new byte[][]{RDATA}));
            System.out.println("NAME[] length:  " + NAME.length);
            System.out.println();
        }

        if (DNSServer.debug == 1) {
            System.out.println("Writing record bytes...");
        }

        DataOutputStream out = new DataOutputStream(stream);

        DNSMessage.writeDomainName(stream, domainNameLocations, NAME);
        out.writeShort(TYPE);
        out.writeShort(CLASS);
        out.writeInt(TTL);
        out.writeShort(RDLENGTH);
        out.write(RDATA, 0, RDATA.length);

        if (DNSServer.debug == 1) {
            System.out.println("Finished writing record bytes!\n");
        }
    }

    boolean timestampValid() {
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());




        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp.getTime());
        calendar.add(Calendar.SECOND, TTL);

        Timestamp timestampAfterTTL = new Timestamp(calendar.getTime().getTime());

        if(DNSServer.debug > 4) {
            System.out.println("CURRENT TIME:       " + currentTime);
            System.out.println("TIMESTAMP RECORDED: " + timestamp);
            System.out.println("TIMESTAMP REQUIRED: " + timestampAfterTTL);
        }

        if(currentTime.compareTo(timestampAfterTTL) < 0) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "DNSRecord{" +
                "NAME=" + Arrays.toString(NAME) +
                ", TYPE=" + TYPE +
                ", CLASS=" + CLASS +
                ", TTL=" + TTL +
                ", RDLENGTH=" + RDLENGTH +
                ", RDATA=" + Arrays.toString(RDATA) +
                '}';
    }

}

