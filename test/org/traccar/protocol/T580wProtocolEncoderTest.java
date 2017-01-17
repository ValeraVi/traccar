package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class T580wProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        T580wProtocolEncoder encoder = new T580wProtocolEncoder();

        Command command;

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        Assert.assertEquals("[begin]sms2,88888888,[end]", encoder.encodeCommand(command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, true);
        Assert.assertEquals("[begin]sms2,*soson*,[end]", encoder.encodeCommand(command));

        command.set(Command.KEY_ENABLE, false);
        Assert.assertEquals("[begin]sms2,*sosoff*,[end]", encoder.encodeCommand(command));

        command = new Command();
        command.setDeviceId(1);
        command.setType("positionRealtime");
        Assert.assertEquals("[begin]sms2,*routetrack*99*,[end]", encoder.encodeCommand(command));

    }

}
