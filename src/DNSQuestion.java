import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * The class that will handle DNS Question section.
 */
public class DNSQuestion {

    // Member Variables
    /**
     * The Labels.
     */
    String[] LABELS;
    /**
     * The Qtype.
     */
    int QTYPE;
    /**
     * The Qclass.
     */
    int QCLASS;

    /**
     * Instantiates a new Dns question.
     *
     * @param stream  the input stream
     * @param message the message object
     * @throws IOException the io exception
     */
    public DNSQuestion(DataInputStream stream, DNSMessage message) throws IOException {

        // Data extracted based on rfc1035
        LABELS = message.readDomainName(stream);
        QTYPE = stream.readUnsignedShort();
        QCLASS = stream.readUnsignedShort();

        if (DNSServer.debug > 0) {
            System.out.println("<--- DECODED QUESTION DATA --->");
            System.out.println("LABELS:         " + Arrays.deepToString(LABELS));
            System.out.println("QTYPE:          " + QTYPE);
            System.out.println("QCLASS:         " + QCLASS);
            System.out.println();

        } // DEBUG
    }

    /**
     * Decode question dns question.
     *
     * @param inputStream the input stream
     * @param message     the message
     * @return the dns question
     * @throws IOException the io exception
     */
    public static DNSQuestion decodeQuestion(ByteArrayInputStream inputStream, DNSMessage message) throws IOException {
        return new DNSQuestion(new DataInputStream(inputStream), message);
    }


    /**
     * Write bytes.
     *
     * @param stream the stream
     * @throws IOException the io exception
     */
    void writeBytes(ByteArrayOutputStream stream, HashMap<String,Integer> domainNameLocations) throws IOException {
        if (DNSServer.debug > 0) {
            System.out.println("<--- DECODED QUESTION DATA --->");
            System.out.println("LABELS:         " + Arrays.deepToString(LABELS));
            System.out.println("QTYPE:          " + QTYPE);
            System.out.println("QCLASS:         " + QCLASS + "\n");

        }

        if (DNSServer.debug == 1) {
            System.out.println("Writing question bytes...");
        }



        /*// Write bytes in format (label length byte followed by the chars in that label)
        for (int j = 0; j < LABELS.length; j++) {
            out.writeByte(LABELS[j].toCharArray().length);
            for (char c : LABELS[j].toCharArray()) {
                out.writeByte(c);
            }
        }*/
//        out.writeByte(0); // LABEL section terminator

        DNSMessage.writeDomainName(stream, domainNameLocations, LABELS );
        DataOutputStream out = new DataOutputStream(stream);
        out.writeShort(QTYPE);
        out.writeShort(QCLASS);

        if (DNSServer.debug == 1) {
            System.out.println("Finished writing header bytes!\n");
        }

    }

    @Override
    public String toString() {
        return "DNSQuestion{" +
                "LABELS=" + Arrays.toString(LABELS) +
                ", QTYPE=" + QTYPE +
                ", QCLASS=" + QCLASS +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSQuestion that = (DNSQuestion) o;
        return QTYPE == that.QTYPE && QCLASS == that.QCLASS && Arrays.equals(LABELS, that.LABELS);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(QTYPE, QCLASS);
        result = 31 * result + Arrays.hashCode(LABELS);
        return result;
    }

}
