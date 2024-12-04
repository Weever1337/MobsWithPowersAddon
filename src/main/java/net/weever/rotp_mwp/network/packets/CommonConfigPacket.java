package net.weever.rotp_mwp.network.packets;

import com.github.standobyte.jojo.network.packets.IModPacketHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.weever.rotp_mwp.Config;

import java.util.function.Supplier;

public class CommonConfigPacket {
    private final Config.Common.SyncedValues values;

    public CommonConfigPacket(Config.Common.SyncedValues values) {
        this.values = values;
    }

    public static class Handler implements IModPacketHandler<CommonConfigPacket> {

        @Override
        public void encode(CommonConfigPacket msg, PacketBuffer buf) {
            msg.values.writeToBuf(buf);
        }

        @Override
        public CommonConfigPacket decode(PacketBuffer buf) {
            return new CommonConfigPacket(new Config.Common.SyncedValues(buf));
        }

        @Override
        public void handle(CommonConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
            msg.values.changeConfigValues();
        }

        @Override
        public Class<CommonConfigPacket> getPacketClass() {
            return CommonConfigPacket.class;
        }
    }
}