package com.benbernard.machineworklist;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.inventory.Container;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import codechicken.nei.NEICPH;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/** Preserve vanilla packet handling and observe completion after the client applies each response. */
final class CraftingTransactions extends ChannelDuplexHandler {

    static final int MAX_TICKS = 200;

    static final class Batch {

        private final Set<Short> pending = new HashSet<>();
        private boolean sealed;
        private boolean rejected;
        private boolean requested;
        private boolean contents;
        private boolean cursor;

        synchronized void sent(short id) {
            // A manual/other-mod click after submission invalidates this request's accounting.
            if (sealed) rejected = true;
            pending.add(id);
        }

        synchronized void confirmed(short id, boolean accepted) {
            if (pending.remove(id) && !accepted) rejected = true;
        }

        synchronized void seal() {
            sealed = true;
        }

        synchronized boolean ready() {
            return finished() && !rejected && contents && cursor;
        }

        synchronized boolean finished() {
            return sealed && (rejected || pending.isEmpty());
        }

        synchronized boolean rejected() {
            return rejected;
        }

        synchronized boolean requestSnapshot() {
            if (!finished() || rejected || requested) return false;
            requested = true;
            return true;
        }

        synchronized void contentsApplied() {
            if (requested) {
                contents = true;
                cursor = false;
            }
        }

        synchronized void cursorApplied() {
            if (contents) cursor = true;
        }
    }

    private final Channel channel;
    private final int window;
    private final int slots;
    private final Runnable requestSnapshot;
    private volatile Batch batch;
    private volatile boolean closed;

    CraftingTransactions(NetworkManager manager, Container container) {
        this(manager, container.windowId, container.inventorySlots.size(), NEICPH::sendRequestContainer);
    }

    CraftingTransactions(NetworkManager manager, int window, int slots, Runnable requestSnapshot) {
        channel = manager.channel();
        this.window = window;
        this.slots = slots;
        this.requestSnapshot = requestSnapshot;
        channel.pipeline()
            .addBefore(
                channel.pipeline()
                    .context(manager)
                    .name(),
                "worklist_transactions",
                this);
    }

    void begin() {
        if (closed || batch != null && !batch.ready())
            throw new IllegalStateException("Inventory transfer still pending");
        batch = new Batch();
    }

    void seal() {
        Batch current = batch;
        channel.eventLoop()
            .execute(current::seal);
    }

    boolean ready() {
        Batch current = batch;
        if (closed || current == null) return false;
        // Called on the client thread. All clicks have now been applied/acknowledged in order.
        // NEI replies with S30 contents followed by S2F cursor; observe both after vanilla applies them.
        if (current.requestSnapshot()) requestSnapshot.run();
        return current.ready();
    }

    boolean rejected() {
        return batch != null && batch.rejected();
    }

    void close() {
        closed = true;
        if (channel.pipeline()
            .context(this) != null)
            channel.pipeline()
                .remove(this);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        Batch current = batch;
        if (current != null && message instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow click = (C0EPacketClickWindow) message;
            if (click.func_149548_c() == window) current.sent(click.func_149547_f());
        }
        super.write(context, message, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        Batch current = batch;
        if (current != null
            && (message instanceof S32PacketConfirmTransaction || message instanceof S30PacketWindowItems
                || message instanceof S2FPacketSetSlot)) {
            Packet packet = (Packet) message;
            // This wrapper never goes on the wire. NetworkManager queues it just like the original.
            super.channelRead(context, new AppliedPacket(packet, () -> applied(current, packet)));
        } else super.channelRead(context, message);
    }

    private void applied(Batch current, Packet packet) {
        if (closed || batch != current) return;
        if (packet instanceof S32PacketConfirmTransaction) {
            S32PacketConfirmTransaction confirmation = (S32PacketConfirmTransaction) packet;
            if (confirmation.func_148889_c() == window)
                current.confirmed(confirmation.func_148890_d(), confirmation.func_148888_e());
        } else if (packet instanceof S30PacketWindowItems) {
            S30PacketWindowItems contents = (S30PacketWindowItems) packet;
            if (contents.func_148911_c() == window && contents.func_148910_d().length == slots)
                current.contentsApplied();
        } else if (packet instanceof S2FPacketSetSlot) {
            S2FPacketSetSlot slot = (S2FPacketSetSlot) packet;
            if (slot.func_149175_c() == -1 && slot.func_149173_d() == -1) current.cursorApplied();
        }
    }

    private static final class AppliedPacket extends Packet {

        private final Packet original;
        private final Runnable after;

        AppliedPacket(Packet original, Runnable after) {
            this.original = original;
            this.after = after;
        }

        @Override
        public void processPacket(INetHandler handler) {
            original.processPacket(handler);
            after.run();
        }

        @Override
        public boolean hasPriority() {
            return original.hasPriority();
        }

        @Override
        public void readPacketData(PacketBuffer data) throws IOException {
            original.readPacketData(data);
        }

        @Override
        public void writePacketData(PacketBuffer data) throws IOException {
            original.writePacketData(data);
        }
    }
}
