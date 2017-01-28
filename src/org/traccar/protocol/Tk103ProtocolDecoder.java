/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Tk103ProtocolDecoder extends BaseProtocolDecoder {

    public Tk103ProtocolDecoder(Tk103Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+),?")                    // device id
            .expression("(.{4}),?")              // command
            .number("(dd)(dd)(dd),?")            // date
            .expression("([AV]),?")              // validity
            .number("(d+)(dd.d+)")               // latitude
            .expression("([NS]),?")
            .number("(d+)(dd.d+)")               // longitude
            .expression("([EW]),?")
            .number("(d+.d)(?:d*,)?")            // speed
            .number("(dd)(dd)(dd),?")            // time
            .number("(d+.?d{1,2}),?").optional() // course
            .number("(d+.?d{1,2}),?").optional() // altitude
            .number("(d+),?").optional()         // rssi
            .text(")").optional()
            .compile();

    private static final Pattern PATTERN_BATTERY = new PatternBuilder()
            .number("(d+),")                     // device id
            .expression(".{4},")                 // command
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time
            .number("(d+),")                     // battery level
            .number("(d+),")                     // battery voltage
            .number("d+,")                       // input voltage
            .number("d+")                        // installed
            .compile();

    private static final Pattern PATTERN_NETWORK = new PatternBuilder()
            .number("(d+),")                     // device id
            .expression("(.{4}),")               // command
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(d+),")                     // lac
            .number("(d+),")                     // cid
            .number("(d+),")                     // number of wifi macs
            .expression("((?:(?:[0-9A-Fa-f]{2}:){5}(?:[0-9A-Fa-f]{2})\\*[-+]?\\d{1,2}\\*\\d{1,2},)*)")
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .any()
            .compile();

    private class BatteryInfo {
        private int level;
        private boolean levelPresent;
        private double voltage;
        private boolean voltagePresent;
        private Date timestamp;

        BatteryInfo() {
            levelPresent = false;
            voltagePresent = false;
            timestamp = null;
        }

        public int getLevel() {
            return level;
        }

        public boolean getLevelPresent() {
            return levelPresent;
        }

        public void setLevel(int level, boolean levelPresent) {
            this.level = level;
            this.levelPresent = levelPresent;
        }

        public double getVoltage() {
            return voltage;
        }

        public boolean getVoltagePresent() {
            return voltagePresent;
        }

        public void setVoltage(double voltage, boolean voltagePresent) {
            this.voltage = voltage;
            this.voltagePresent = voltagePresent;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "{batLev=" + (levelPresent ? String.valueOf(level) : "none")
                    + ", batVolt=" + (voltagePresent ? String.valueOf(voltage) : "none")
                    + ", time=" + timestamp
                    + "}";
        }
    }

    private static Map<Long, BatteryInfo> batteryInfo = new HashMap<Long, BatteryInfo>();

    public void getLastBatteryPower(Position position) {
        if (position.getDeviceId() != 0) {
            BatteryInfo bi = batteryInfo.get(position.getDeviceId());
            if (bi != null) {
                if (bi.getLevelPresent()) {
                    position.set(Position.KEY_POWER, bi.getLevel());
                }
                if (bi.getVoltagePresent()) {
                    position.set(Position.KEY_BATTERY, bi.getVoltage());
                }
            }
        }
    }

    private void decodeAlarmStatus(Position position, String command) {
        switch (command) {
            // Motion Alert with Location
            case "ZC11":
            case "DW31":
            case "DW51":
                position.set(Position.KEY_ALARM, Position.ALARM_MOVEMENT);
                break;
            // Low Battery Alert with Location
            case "ZC12":
            case "DW32":
            case "DW52":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                break;
            // Power Cut Alert with Location
            case "ZC13":
            case "DW33":
            case "DW53":
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
                break;
            // Dismounting Alert with Location
            case "ZC17":
            case "DW37":
            case "DW57":
                position.set(Position.KEY_ALARM, "dismounting");
                break;
            // SOS Alert with Location
            case "ZC25":
            case "DW3E":
            case "DW5E":
                position.set(Position.KEY_ALARM,  Position.ALARM_SOS);
                break;
            // Open Alert with Location:
            case "ZC26":
            case "DW3F":
            case "DW5F":
                position.set(Position.KEY_ALARM, "opened");
                break;
            // Low Voltage Alert with Location
            case "ZC27":
            case "DW40":
            case "DW60":
                position.set(Position.KEY_ALARM, "lowVoltage");
                break;
            // Bad Battery Alert with Location
            case "ZC28":
            case "DW41":
            case "DW61":
                position.set(Position.KEY_ALARM, "badBattery");
                break;
            // Ignition Alert with Location
            case "ZC29":
            case "DW42":
            case "DW62":
                position.set(Position.KEY_IGNITION, true);
                break;
            // ACC On Alert with Location
            case "ZC15":
            case "DW35":
            case "DW55":
                position.set(Position.KEY_IGNITION, true);
                break;
            // ACC Off Alert with Location
            case "ZC16":
            case "DW36":
            case "DW56":
                position.set(Position.KEY_IGNITION, false);
                break;
            // Fuel Cut On with Location
            case "ZC2A":
            case "DW43":
            case "DW63":
                position.set(Position.KEY_FUEL, 0.0);
                break;
            // Fuel Cut Off with Location:
            case "ZC2B":
            case "DW44":
            case "DW64":
                position.set(Position.KEY_FUEL, 1.0);
                break;
            // Fuel Cut Alert with Location:
            case "ZC2C":
            case "DW45":
            case "DW65":
                position.set(Position.KEY_ALARM, "fuelCut");
                break;
            default:
                break;
        }
    }

    private boolean decodeBattery(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_BATTERY, sentence);
        if (parser.matches()) {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession == null) {
                return false;
            }
            BatteryInfo bi = new BatteryInfo();

            DateBuilder dateBuilder = new DateBuilder()
                    .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

            bi.setTimestamp(dateBuilder.getDate());

            int level = parser.nextInt();
            bi.setLevel(level, level != 255);

            int battery = parser.nextInt();
            bi.setVoltage(battery * 0.01, battery != 65535);

            //Just store the currnet battery level for current device ID.
            // It will be used later to attach the battery level information to the real position message
            // We will not creating the fake position message with "last" position, as last position may be
            // not valid, outdated, not ready (if it is from geolocation service which is not finished), etc.
            // Also fake position messages will increase load on geolocation service, wasting the quota in
            // subscription in such services. Also there will be no double events with same timestamps in Reports.
            //The only drawback of this method is that battery information in each event will be old, i.e. from
            // previous event in fact.
            batteryInfo.put(deviceSession.getDeviceId(), bi);

            return true;
        }
        return false;
    }

    private boolean decodeLbsWifi(Channel channel, SocketAddress remoteAddress, Position position, String sentence) {
        Parser parser = new Parser(PATTERN_NETWORK, sentence);
        if (parser.matches()) {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession == null) {
                return false;
            }
            position.setDeviceId(deviceSession.getDeviceId());
            decodeAlarmStatus(position, parser.next());

            getLastLocation(position, null);
            // If geolocation service will fail to return the lat/lon, it will be possible to filter such
            //  events by "filter.invalid" configuration option.
            position.setValid(false);

            getLastBatteryPower(position);

            Network network = new Network();

            // Parse LBS
            network.addCellTower(CellTower.from(
                    parser.nextInt(), parser.nextInt(), parser.nextInt(), parser.nextInt()));

            // Parse WiFi macs number and mac addresses itself.
            int wifiCount = parser.nextInt();
            String[] wifimacs = parser.next().split(",");
            if (wifimacs.length == wifiCount) {
                for (int i = 0; i < wifiCount; i++) {
                    // Sample wifi string: “00:80:E1:7F:86:97*-55*6” (mac*rssi*channel_number)
                    String[] wifiinfo = wifimacs[i].split("\\*");
                    network.addWifiAccessPoint(WifiAccessPoint.from(
                            wifiinfo[0], Integer.parseInt(wifiinfo[1]), Integer.parseInt(wifiinfo[2])));
                }
            }

            if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
                position.setNetwork(network);
            }

            // Parse date-time
            DateBuilder dateBuilder = new DateBuilder()
                    .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            // Set both DeviceTime and FixTime here, as some alarm messages like SOS at device start may be
            //  sent without valid LBS/WiFi position, so FixTime from getLastLocation will be used as time
            //  for this SOS alarm, which can be very old. And traccar UI only display FixTime.
            // Valid flag will be "false" anyway, so it is possible to check should the position and fixTime
            //   be trusted or not.
            position.setTime(dateBuilder.getDate());

            return true;
        }
        return false;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        // Find message start
        int beginIndex = sentence.indexOf('(');
        if (beginIndex != -1) {
            sentence = sentence.substring(beginIndex + 1);
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (decodeBattery(channel, remoteAddress, sentence)) {
            return null;
        }

        if (decodeLbsWifi(channel, remoteAddress, position, sentence)) {
            return position;
        }

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());
        decodeAlarmStatus(position, parser.next());

        getLastBatteryPower(position);

        DateBuilder dateBuilder = new DateBuilder();
        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        switch (Context.getConfig().getString(getProtocolName() + ".speed", "kmh")) {
            case "kn":
                position.setSpeed(parser.nextDouble());
                break;
            case "mph":
                position.setSpeed(UnitsConverter.knotsFromMph(parser.nextDouble()));
                break;
            default:
                position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
                break;
        }

        dateBuilder.setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        if (parser.hasNext()) {
            position.setCourse(parser.nextDouble());
            if (parser.hasNext()) {
                position.setAltitude(parser.nextDouble());
                if (parser.hasNext()) {
                    position.set(Position.KEY_RSSI, parser.nextInt());
                }
            }
        }
        position.setAccuracy(5.0);

        return position;
    }

}
