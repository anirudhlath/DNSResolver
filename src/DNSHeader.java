import java.io.*;

public class DNSHeader {
    int ID;
    int FLAGS;
    int QR;
    int OPCODE;
    int AA;
    int TC;
    int RD;
    int RA;
    int Z;
    int RCODE;
    int QDCOUNT;
    int ANCOUNT;
    int NSCOUNT;
    int ARCOUNT;

    final int byteCount = 12;

    public DNSHeader(int ID,
                     int QR,
                     int OPCODE,
                     int AA,
                     int TC,
                     int RD,
                     int RA,
                     int Z,
                     int RCODE,
                     int QDCOUNT,
                     int ANCOUNT,
                     int NSCOUNT,
                     int ARCOUNT) {
        this.ID = ID;
        this.FLAGS = 0;
        this.QR = QR;
        this.OPCODE = OPCODE;
        this.AA = AA;
        this.TC = TC;
        this.RD = RD;
        this.RA = RA;
        this.Z = Z;
        this.RCODE = RCODE;
        this.QDCOUNT = QDCOUNT;
        this.ANCOUNT = ANCOUNT;
        this.NSCOUNT = NSCOUNT;
        this.ARCOUNT = ARCOUNT;
    }

    public DNSHeader(DataInputStream stream) throws IOException {
        // Data extracted based on rfc1035
        ID = stream.readUnsignedShort();

        FLAGS = stream.readUnsignedShort();
        QR = FLAGS & 0b1000000000000000;
        OPCODE = FLAGS & 0b0111100000000000;
        AA = FLAGS & 0b0000010000000000;
        TC = FLAGS & 0b0000001000000000;
        RD = FLAGS & 0b0000000100000000;
        RA = FLAGS & 0b0000000010000000;
        Z = FLAGS & 0b0000000001110000;
        RCODE = FLAGS & 0b0000000000001111;

        QDCOUNT = stream.readUnsignedShort();
        ANCOUNT = stream.readUnsignedShort();
        NSCOUNT = stream.readUnsignedShort();
        ARCOUNT = stream.readUnsignedShort();

        if (DNSServer.debug > 0) {
            System.out.println("<--- DECODED HEADER DATA --->");
            System.out.println("ID:         " + ID);
            System.out.println("QR:         " + QR);
            System.out.println("OPCODE:     " + OPCODE);
            System.out.println("AA:         " + AA);
            System.out.println("TC:         " + TC);
            System.out.println("RD:         " + RD);
            System.out.println("RA:         " + RA);
            System.out.println("Z:          " + Z);
            System.out.println("RCODE:      " + RCODE);
            System.out.println("QDCOUNT:    " + QDCOUNT);
            System.out.println("ANCOUNT:    " + ANCOUNT);
            System.out.println("NSCOUNT:    " + NSCOUNT);
            System.out.println("ARCOUNT:    " + ARCOUNT + "\n");
        }
    }

    public static DNSHeader decodeHeader(ByteArrayInputStream inputStream) throws IOException {
        return new DNSHeader(new DataInputStream(inputStream));
    }


    static DNSHeader buildResponseHeader(DNSMessage request, DNSMessage response) {
        DNSHeader result = new DNSHeader(request.header.ID, 0b1000000000000000, request.header.OPCODE,
                request.header.AA,
                request.header.TC, request.header.RD, request.header.RA, request.header.Z, request.header.RCODE,
                request.header.QDCOUNT, request.header.QDCOUNT, request.header.NSCOUNT, request.header.ARCOUNT);
        response.header = result;

        if (DNSServer.debug == 1) {
            System.out.println("<--- DECODED HEADER DATA --->");
            System.out.println("ID:         " + result.ID);
            System.out.println("QR:         " + result.QR);
            System.out.println("OPCODE:     " + result.OPCODE);
            System.out.println("AA:         " + result.AA);
            System.out.println("TC:         " + result.TC);
            System.out.println("RD:         " + result.RD);
            System.out.println("RA:         " + result.RA);
            System.out.println("Z:          " + result.Z);
            System.out.println("RCODE:      " + result.RCODE);
            System.out.println("QDCOUNT:    " + result.QDCOUNT);
            System.out.println("ANCOUNT:    " + result.ANCOUNT);
            System.out.println("NSCOUNT:    " + result.NSCOUNT);
            System.out.println("ARCOUNT:    " + result.ARCOUNT + "\n");
        }

        if (DNSServer.debug > 1) {
            System.out.println(request.header.toString());
            System.out.println(response.header.toString());
            System.out.println(Integer.toBinaryString(request.header.FLAGS));
        }
        return result;

    }

    void writeBytes(ByteArrayOutputStream stream) throws IOException {
        if (DNSServer.debug == 1) {
            System.out.println("Writing header bytes...");
        }

        DataOutputStream outputStream = new DataOutputStream(stream);
        outputStream.writeShort(this.ID);

        this.FLAGS = 0;
        FLAGS |= QR;
        FLAGS |= OPCODE;
        FLAGS |= AA;
        FLAGS |= TC;
        FLAGS |= RD;
        FLAGS |= RA;
        FLAGS |= Z;
        FLAGS |= RCODE;

        if (DNSServer.debug > 1) {
            System.out.println("<--- CREATING RESPONSE HEADER --->");
            String response = this.toString();
            System.out.println(response);
            System.out.println("FLAGS Binary Value:     " + Integer.toBinaryString(FLAGS));
        }

        outputStream.writeShort(FLAGS);
        outputStream.writeShort(QDCOUNT);
        outputStream.writeShort(ANCOUNT);
        outputStream.writeShort(NSCOUNT);
        outputStream.writeShort(ARCOUNT);

        if (DNSServer.debug == 1) {
            System.out.println("Finished writing header bytes!\n");
        }


    }

    @Override
    public String toString() {
        return "DNSHeader{" +
                "ID=" + ID +
                ", FLAGS=" + FLAGS +
                ", QR=" + QR +
                ", OPCODE=" + OPCODE +
                ", AA=" + AA +
                ", TC=" + TC +
                ", RD=" + RD +
                ", RA=" + RA +
                ", Z=" + Z +
                ", RCODE=" + RCODE +
                ", QDCOUNT=" + QDCOUNT +
                ", ANCOUNT=" + ANCOUNT +
                ", NSCOUNT=" + NSCOUNT +
                ", ARCOUNT=" + ARCOUNT +
                ", byteCount=" + byteCount +
                '}';
    }

}
