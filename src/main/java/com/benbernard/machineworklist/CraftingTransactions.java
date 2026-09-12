package com.benbernard.machineworklist;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/** Observe vanilla acknowledgments; never suppress, modify or manufacture inventory packets. */
final class CraftingTransactions extends ChannelDuplexHandler {

    static final class Batch {

        private final Set<Short> pending = new HashSet<>();
        private boolean sealed;
        private boolean rejected;

        synchronized void sent(short id) {
            pending.add(id);
        }

        synchronized void confirmed(short id, boolean accepted) {
            if (pending.remove(id) && !accepted) rejected = true;
        }

        synchronized void seal() {
            sealed = true;
        }

        synchronized boolean ready() {
            return sealed && pending.isEmpty() && !rejected;
        }

        synchronized boolean finished() {
            return sealed && (rejected || pending.isEmpty());
        }

        synchronized boolean rejected() {
            return rejected;
        }
    }

    private final Channel channel;
    private final int window;
    private volatile Batch batch;
    private boolean observedReady;

    CraftingTransactions(NetworkManager manager, int window) {
        this.channel = manager.channel();
        this.window = window;
        channel.pipeline()
            .addBefore(
                channel.pipeline()
                    .context(manager)
                    .name(),
                "worklist_transactions",
                this);
    }

    void begin() {
        batch = new Batch();
        observedReady = false;
    }

    void seal() {
        Batch current = batch;
        // Runs after all writes submitted by this client-thread transfer.
        channel.eventLoop()
            .execute(current::seal);
    }

    boolean ready() {
        if (batch == null || !batch.finished()) return false;
        // Netty observes packets before the main thread applies queued inventory snapshots.
        // Require a subsequent client tick before reading authoritative slots or recovering tools.
        boolean previous = observedReady;
        observedReady = true;
        return previous;
    }

    boolean rejected() {
        return batch != null && batch.rejected();
    }

    void close() {
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
        if (current != null && message instanceof S32PacketConfirmTransaction) {
            S32PacketConfirmTransaction confirmation = (S32PacketConfirmTransaction) message;
            if (confirmation.func_148889_c() == window)
                current.confirmed(confirmation.func_148890_d(), confirmation.func_148888_e());
        }
        super.channelRead(context, message);
    }
}
