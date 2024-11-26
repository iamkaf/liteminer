package com.iamkaf.liteminer.networking;

import com.iamkaf.liteminer.Liteminer;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class LiteminerNetwork {
    public static final SimpleNetworkManager NET = SimpleNetworkManager.create(Liteminer.MOD_ID);
    public static MessageType VEINMINE_KEYBIND_CHANGE =
            NET.registerC2S("veinmine_keybind_change", Messages.C2SVeinmineKeybindChange::new);

    public static void init() {
    }

    public static class Messages {
        public static class C2SVeinmineKeybindChange extends BaseC2SMessage {
            private final boolean keybindState;
            private final int shape;

            public C2SVeinmineKeybindChange(boolean keybindState, int shape) {
                this.keybindState = keybindState;
                this.shape = shape;
            }

            public C2SVeinmineKeybindChange(FriendlyByteBuf buf) {
                this.keybindState = buf.readBoolean();
                this.shape = buf.readInt();
            }

            @Override
            public MessageType getType() {
                return VEINMINE_KEYBIND_CHANGE;
            }

            @Override
            public void write(RegistryFriendlyByteBuf buf) {
                buf.writeBoolean(keybindState);
                buf.writeInt(shape);
            }

            public void encode(FriendlyByteBuf buf) {
                buf.writeBoolean(keybindState);
                buf.writeInt(shape);
            }

            @Override
            public void handle(NetworkManager.PacketContext context) {
                Liteminer.instance.onKeymappingStateChange((ServerPlayer) context.getPlayer(), keybindState, shape);
            }

            public void apply(Supplier<NetworkManager.PacketContext> context) {
                Liteminer.instance.onKeymappingStateChange((ServerPlayer) context.get().getPlayer(),
                        keybindState,
                        shape
                );
            }
        }
    }
}
