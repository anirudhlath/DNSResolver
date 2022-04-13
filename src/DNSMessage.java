import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DNSMessage {
    static byte[] receiveData;
    static byte[] backupData;
    DNSHeader header;
    DNSQuestion[] questions;
    DNSRecord[] answers;
    DNSRecord[] authorityRecords;
    DNSRecord[] additionalRecords;
    HashMap<String,Integer> domainNameLocations = new HashMap<>();

    public DNSMessage(ByteArrayInputStream stream) throws IOException {
        if (DNSServer.debug > 0) {
            System.out.println("REQUEST STARTED -------------------------------------------------------------------->");
        }

        // Header Section
        header = DNSHeader.decodeHeader(stream);

        // Question Section
        questions = new DNSQuestion[header.QDCOUNT];
        for (int i = 0; i < header.QDCOUNT; i++) {
            questions[i] = DNSQuestion.decodeQuestion(stream, this);
        }

        // Answer Section
        answers = new DNSRecord[header.ANCOUNT];
        for (int i = 0; i < header.ANCOUNT; i++) {
            try {
                answers[i] = DNSRecord.decodeRecord(stream, this);
            } catch (EOFException e) {
                stream = new ByteArrayInputStream(backupData);
                additionalRecords[i] = DNSRecord.decodeRecord(stream, this);
            }
        }

        // Authority Section
        authorityRecords = new DNSRecord[header.NSCOUNT];
        for (int i = 0; i < header.NSCOUNT; i++) {
            try {
                authorityRecords[i] = DNSRecord.decodeRecord(stream, this);
            } catch (EOFException e) {
                stream = new ByteArrayInputStream(backupData);
                additionalRecords[i] = DNSRecord.decodeRecord(stream, this);
            }
        }

        // Additional Section
        additionalRecords = new DNSRecord[header.ARCOUNT];
        for (int i = 0; i < header.ARCOUNT; i++) {
            try {
                additionalRecords[i] = DNSRecord.decodeRecord(stream, this);
            } catch (EOFException e) {
                stream = new ByteArrayInputStream(backupData);
                additionalRecords[i] = DNSRecord.decodeRecord(stream, this);
            }
        }

        if (DNSServer.debug > 0) {
            System.out.println("<--- MESSAGE STATISTICS --->");
            System.out.println("HEADER:         " + header.toString());
            System.out.println("QUESTIONS:      " + Arrays.stream(questions).toList().toString());
            System.out.println("ANSWERS:        " + Arrays.stream(answers).toList().toString());
            System.out.println("AUTHORITY:      " + Arrays.stream(authorityRecords).toList().toString());
            System.out.println("ADDITIONAL:     " + Arrays.stream(additionalRecords).toList().toString());
        }

        if (DNSServer.debug > 0) {
            System.out.println("REQUEST ENDED " +
                    "---------------------------------------------------------------------->" + "\n");
        }
    }

    public DNSMessage(DNSMessage request, DNSRecord[] answers) {
        if (DNSServer.debug > 0) {
            System.out.println("RESPONSE STARTED " +
                    "-------------------------------------------------------------------->");
        }
        this.header = DNSHeader.buildResponseHeader(request, this);
        this.questions = request.questions;
        if (this.header.ANCOUNT > 0) {
            this.answers = answers;
            this.authorityRecords = request.authorityRecords;
        } else {
            this.authorityRecords = answers;
            this.answers = request.answers;
        }

        this.additionalRecords = request.additionalRecords;



    }

    public static DNSMessage decodeMessage(byte[] receiveData) throws IOException {
        DNSMessage.receiveData = receiveData;
        return new DNSMessage(new ByteArrayInputStream(receiveData));
    }

    String[] readDomainName(DataInputStream stream) throws IOException {
        List<String> labels = new ArrayList<>();

        byte[] temp = new byte[2];
        temp[0] = stream.readByte();
        int firstOctet = temp[0];

        int offset;

        if ((firstOctet & 0b11000000) == 192) {
            temp[1] = stream.readByte();
            DataInputStream tempStream = new DataInputStream(new ByteArrayInputStream(temp));
            int pointer = tempStream.readShort();
            tempStream.close();

            offset = pointer & 0b0011111111111111;

            if (DNSServer.debug > 0) {
                System.out.println("<--- COMPRESSION FOUND --->");
                System.out.println("OFFSET:         " + offset);
            }
            labels = Arrays.stream(readDomainName(offset)).toList();
        } else {
            if (firstOctet != 0) {
                int charCount = firstOctet;
                StringBuilder label = new StringBuilder();
                for (int i = 0; i <= charCount; i++) {
                    if (i != charCount) {
                        char c = (char) stream.readUnsignedByte();
                        label.append(c);
                    } else {
                        labels.add(label.toString());
                        label = new StringBuilder();
                        charCount = stream.readUnsignedByte();
                        i = -1; // Offset -1 was necessary I am not exactly sure why

                        if (charCount == 0) {
                            break;
                        }
                    }
                }
            }
        }
        return labels.toArray(new String[0]);
    }

    String[] readDomainName(int firstByte) throws IOException {
        return readDomainName(new DataInputStream(new ByteArrayInputStream(receiveData, firstByte,
                receiveData.length)));
    }

    static DNSMessage buildResponse(DNSMessage request, DNSRecord[] answers) throws IOException {
        return new DNSMessage(request, answers);
    }

    byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        header.writeBytes(out);

        for (int i = 0; i < questions.length; i++) {
            questions[i].writeBytes(out, this.domainNameLocations);
        }

        for (int i = 0; i < answers.length; i++) {
            answers[i].writeBytes(out, this.domainNameLocations);
        }

        for (int i = 0; i < authorityRecords.length; i++) {
            authorityRecords[i].writeBytes(out, this.domainNameLocations);
        }

        for (int i = 0; i < additionalRecords.length; i++) {
            additionalRecords[i].writeBytes(out, this.domainNameLocations);
        }
        out.close();

        if (DNSServer.debug > 0) {
            System.out.println("<--- MESSAGE STATISTICS --->");
            System.out.println("HEADER:         " + header.toString());
            System.out.println("QUESTIONS:      " + Arrays.stream(questions).toList().toString());
            System.out.println("ANSWERS:        " + Arrays.stream(answers).toList().toString());
            System.out.println("AUTHORITY:      " + Arrays.stream(authorityRecords).toList().toString());
            System.out.println("ADDITIONAL:     " + Arrays.stream(additionalRecords).toList().toString() + "\n");
        }

        if (DNSServer.debug > 0) {
            System.out.println("RESPONSE ENDED " +
                    "---------------------------------------------------------------------->" + "\n");
        }
        return out.toByteArray();
    }

    static void writeDomainName(ByteArrayOutputStream stream, HashMap<String,Integer> domainNameLocations,
                          String[] domainPieces) throws IOException {
        DataOutputStream out = new DataOutputStream(stream);
        String temp = octetsToString(domainPieces);
        if(domainNameLocations.containsKey(temp)) {
            int offset = domainNameLocations.get(temp);
            short tempByte = (short) offset;
            tempByte |= 0b1100000000000000;
        } else {
            for (int j = 0; j < domainPieces.length; j++) {
                out.writeByte(domainPieces[j].toCharArray().length);
                for (char c : domainPieces[j].toCharArray()) {
                    out.writeByte(c);
                }
            }
            out.writeByte(0);
        }

    }

    static String octetsToString(String[] octets) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < octets.length; i++) {
            builder.append(octets[i]);
            builder.append('.');
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "DNSMessage{" +
                "header=" + header +
                ", questions=" + Arrays.toString(questions) +
                ", answers=" + Arrays.toString(answers) +
                ", authorityRecords=" + Arrays.toString(authorityRecords) +
                ", additionalRecords=" + Arrays.toString(additionalRecords) +
                '}';
    }


}
