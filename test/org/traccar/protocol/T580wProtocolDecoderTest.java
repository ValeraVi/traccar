package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class T580wProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        T580wProtocolDecoder decoder = new T580wProtocolDecoder(new T580wProtocol());

        verifyPosition(decoder, text(
                "(864768010869060,DW30,050117,A,5135.82713N,00001.17918E,0.089,154745,000.0,43.40,12)"));

        verifyPosition(decoder, text(
                "\n\n\n(864768010869060,DW30,050117,A,5135.82713N,00001.17918E,0.089,154745,000.0,43.40,12)"));

        verifyNothing(decoder, text(
                "(013632651491,ZC20,180716,144222,6,392,65535,255"));
        verifyAttributes(decoder, text(
                "(013632651491,DW30,050117,A,5135.82713N,00001.17918E,0.089,154745)"));

        verifyPosition(decoder, text(
                "(864768010887682,DW5B,240,1,5015,40601,3,30:15:A8:E7:86:C8*-55*11,42:66:DE:EA:BC:2E*-78*1,4C:60:DE:0A:BB:22*-79*1,051216,014802"));

        verifyNothing(decoder, text(
                "(013632651492,ZC20,040613,040137,6,421,112,0"));
        verifyAttributes(decoder, text(
                "(013632651492,DW30,050117,A,5135.82713N,00001.17918E,0.089,154745)"));

        verifyNothing(decoder, text(
                "(864768010159785,ZC20,291015,030413,3,362,65535,255"));
        verifyAttributes(decoder, text(
                "(864768010159785,DW30,050117,A,5135.82713N,00001.17918E,0.089,154745)"));

        verifyPosition(decoder, text(
                "(352606090042050,BP05,240414,V,0000.0000N,00000.0000E,000.0,193133,000.0"));

        verifyPosition(decoder, text(
                "(352606090042050,BP05,240414,A,4527.3513N,00909.9758E,4.80,112825,155.49"),
                position("2014-04-24 11:28:25.000", true, 45.45586, 9.16626));

        verifyPosition(decoder, text(
                "(864768010009188,BP05,271114,V,4012.19376N,00824.05638E,000.0,154436,000.0"));

        verifyPosition(decoder, text(
                "(013632651491,BP05,040613,A,2234.0297N,11405.9101E,000.0,040137,178.48)"));

        verifyPosition(decoder, text(
                "(013632651491,ZC07,040613,A,2234.0297N,11405.9101E,000.0,040137,178.48)"));

        verifyAttributes(decoder, text(
                "(013632651491,ZC11,040613,A,2234.0297N,11405.9101E,000.0,040137,178.48)"));

        verifyAttributes(decoder, text(
                "(013632651491,ZC12,040613,A,2234.0297N,11405.9101E,000.0,040137,178.48)"));

        verifyAttributes(decoder, text(
                "(013632651491,ZC13,040613,A,2234.0297N,11405.9101E,000.0,040137,178.48)"));

        verifyAttributes(decoder, text(
                "(013632651491,ZC17,040613,A,2234.0297N,11405.9101E,000.0,040137,178.48)"));

        verifyNothing(decoder, text(
                "(013632651493,ZC20,040613,040137,6,42,112,0)"));
        verifyAttributes(decoder, text(
                "(013632651493,DW30,050117,A,5135.82713N,00001.17918E,0.089,154745)"));

    }

}
