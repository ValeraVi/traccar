/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;


public class T580wProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    @Override
    public String formatValue(String key, Object value) {
        return null;
    }

    protected String formatCommand(Command command, String format, String... keys) {
        String content = super.formatCommand(command, format, this, keys);
        return String.format("[begin]sms2,%s,[end]", content);
    }

    private String getEnableFlag(Command command) {
        if (command.getBoolean(Command.KEY_ENABLE)) {
            return "on";
        } else {
            return "off";
        }
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_POSITION_SINGLE:
                return formatCommand(command, "*getposl*");
            case "positionRealtime":
                return formatCommand(command, "*routetrack*99*");
            case "positionRealtimeStop":
                return formatCommand(command, "*routetrackoff*");
            case "modeDeepSleepInterval2Hour":
                return formatCommand(command, "*sleep*2*");
            case "modeDeepSleepMotion":
                return formatCommand(command, "*sleepv*");
            case "modeDeepSleepOff":
                return formatCommand(command, "*sleepoff*");
            case "alarmSosOn":
                return formatCommand(command, "*soson*");
            case "alarmSosOff":
                return formatCommand(command, "*sosoff*");
            case Command.TYPE_ALARM_SOS:
                return formatCommand(command, "*sos" + getEnableFlag(command) + "*");
            case "multiControllerOn":
                return formatCommand(command, "*multiquery*");
            case "multiControllerOff":
                return formatCommand(command, "*multiqueryoff*");
            case Command.TYPE_REBOOT_DEVICE:
                return formatCommand(command, "88888888");
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
