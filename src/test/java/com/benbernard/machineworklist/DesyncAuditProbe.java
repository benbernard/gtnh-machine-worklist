package com.benbernard.machineworklist;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import codechicken.nei.api.IGuiContainerOverlay;
import codechicken.nei.recipe.ContainerRecipe;
import io.netty.channel.embedded.EmbeddedChannel;

/** Regression fixtures using the real Minecraft receive queue and container packet handlers. */
public final class DesyncAuditProbe {

    private static final class Connection {

        final NetworkManager network = new NetworkManager(true);
        final EmbeddedChannel channel = new EmbeddedChannel(network);
        final CraftingTransactions transactions = new CraftingTransactions(network, 7, 1, () -> this.requests++);
        int slotsApplied;
        int confirmationsApplied;
        int requests;

        Connection() {
            network.setNetHandler(
                (INetHandlerPlayClient) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { INetHandlerPlayClient.class },
                    (proxy, method, args) -> {
                        if (method.getName()
                            .equals("handleSetSlot")) slotsApplied++;
                        if (method.getName()
                            .equals("handleConfirmTransaction")) confirmationsApplied++;
                        return null;
                    }));
            transactions.begin();
            channel.writeOutbound(new C0EPacketClickWindow(7, 0, 0, 1, null, (short) 23));
            transactions.seal();
            channel.runPendingTasks();
        }

        void close() {
            transactions.close();
            assertNull(
                channel.pipeline()
                    .context("worklist_transactions"));
            channel.finish();
        }

        void snapshot(int window, int slots) {
            channel.writeInbound(
                new S30PacketWindowItems(
                    window,
                    java.util.Collections.nCopies(slots, (net.minecraft.item.ItemStack) null)));
        }

        void cursor() {
            channel.writeInbound(new S2FPacketSetSlot(-1, -1, null));
        }
    }

    static void acknowledgmentBeforeDelayedSlots() {
        Connection c = new Connection();
        try {
            c.channel.writeInbound(new S32PacketConfirmTransaction(7, (short) 23, true));
            c.network.processReceivedPackets();
            assertFalse(c.transactions.ready());
            assertEquals(1, c.requests);
            c.network.processReceivedPackets(); // next tick, final slot packet has not arrived
            for (int i = 0; i < 20; i++) assertFalse(c.transactions.ready());
            assertEquals(1, c.confirmationsApplied);
            assertEquals(0, c.slotsApplied);
            c.channel.writeInbound(new S2FPacketSetSlot(7, 1, null));
            c.network.processReceivedPackets();
            assertEquals(1, c.slotsApplied);
            assertFalse(c.transactions.ready()); // a single grid update is not the complete response
            c.snapshot(7, 1);
            assertFalse(c.transactions.ready()); // received on Netty but not applied
            c.network.processReceivedPackets();
            assertFalse(c.transactions.ready()); // full contents still need the matching cursor response
            c.cursor();
            assertFalse(c.transactions.ready());
            c.network.processReceivedPackets();
            assertTrue(c.transactions.ready());
            assertEquals(1, c.requests);
        } finally {
            c.close();
        }
    }

    static void acknowledgmentBeforeQueuedPacketsApplied() {
        Connection c = new Connection();
        try {
            // Unrelated world traffic can already be queued ahead of the inventory response.
            for (int i = 0; i < 2500; i++) c.channel.writeInbound(new S32PacketConfirmTransaction(8, (short) i, true));
            c.channel.writeInbound(new S32PacketConfirmTransaction(7, (short) 23, true));
            c.channel.writeInbound(new S2FPacketSetSlot(7, 1, null));
            c.network.processReceivedPackets();
            assertFalse(c.transactions.ready());
            c.network.processReceivedPackets();
            assertFalse(c.transactions.ready());
            assertEquals(0, c.requests);
            assertEquals(2002, c.confirmationsApplied);
            assertEquals(0, c.slotsApplied);
            c.network.processReceivedPackets();
            assertEquals(2501, c.confirmationsApplied);
            assertEquals(1, c.slotsApplied);
            assertFalse(c.transactions.ready());
            assertEquals(1, c.requests);
            c.snapshot(7, 1);
            c.cursor();
            c.network.processReceivedPackets();
            assertTrue(c.transactions.ready());
        } finally {
            c.close();
        }
    }

    static void recipeReturnRestoresRealContainer() throws Exception {
        // Allocate headless objects: no Minecraft constructor, OpenGL, world or account access.
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
        Minecraft mc = (Minecraft) unsafe.allocateInstance(Minecraft.class);
        mc.thePlayer = (EntityClientPlayerMP) unsafe.allocateInstance(EntityClientPlayerMP.class);
        net.minecraft.inventory.InventoryBasic inventory = new net.minecraft.inventory.InventoryBasic("audit", true, 1);
        Container real = new Container() {

            {
                addSlotToContainer(new net.minecraft.inventory.Slot(inventory, 0, 0, 0));
            }

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        };
        real.windowId = 7;
        GuiContainer original = new GuiContainer(real) {

            @Override
            protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {}
        };
        original.mc = mc;
        original.initGui();
        assertSame(real, mc.thePlayer.openContainer);

        WorklistGui worklist = new WorklistGui() {

            @Override
            protected GuiScreen parentScreen() {
                return original;
            }
        };
        worklist.mc = mc;
        mc.currentScreen = worklist;
        GuiContainer destination = worklist.recipeReturnTarget(mc.thePlayer, mc.currentScreen);
        assertSame(original, destination);
        assertFalse(IGuiContainerOverlay.class.isAssignableFrom(WorklistGui.class));

        // NEI is now opened from the real container, so its normal return target restores the window.
        mc.thePlayer.openContainer = new ContainerRecipe();
        destination.initGui();
        assertSame(real, mc.thePlayer.openContainer);

        java.util.List<net.minecraft.network.Packet> replies = new java.util.ArrayList<>();
        net.minecraft.client.network.NetHandlerPlayClient handler = new net.minecraft.client.network.NetHandlerPlayClient(
            mc,
            null,
            null) {

            @Override
            public void addToSendQueue(net.minecraft.network.Packet packet) {
                replies.add(packet);
            }
        };
        handler.handleSetSlot(
            new S2FPacketSetSlot(7, 0, new net.minecraft.item.ItemStack(net.minecraft.init.Items.stick)));
        handler.handleConfirmTransaction(new S32PacketConfirmTransaction(7, (short) 24, false));
        assertNotNull(inventory.getStackInSlot(0));
        assertEquals(1, replies.size());
        assertTrue(replies.get(0) instanceof net.minecraft.network.play.client.C0FPacketConfirmTransaction);

        assertTrue(CraftingInventory.usableFrom(original, worklist, mc.thePlayer));
        assertFalse(CraftingInventory.usableFrom(original, null, mc.thePlayer));
        assertFalse(CraftingInventory.usableFrom(original, new GuiScreen(), mc.thePlayer));
        mc.thePlayer.openContainer = new ContainerRecipe();
        mc.thePlayer.openContainer.windowId = 7; // matching numeric IDs do not make an old container valid
        assertFalse(CraftingInventory.usableFrom(original, worklist, mc.thePlayer));
        assertNull(worklist.recipeReturnTarget(mc.thePlayer, worklist));
    }

    static void wrongSnapshotAndRejectionNeverAdvance() {
        Connection c = new Connection();
        try {
            c.channel.writeInbound(new S32PacketConfirmTransaction(7, (short) 23, true));
            c.network.processReceivedPackets();
            assertFalse(c.transactions.ready());
            c.snapshot(8, 1);
            c.cursor();
            c.snapshot(7, 2);
            c.cursor();
            c.network.processReceivedPackets();
            assertFalse(c.transactions.ready());
            c.snapshot(7, 1);
            c.cursor();
            c.network.processReceivedPackets();
            assertTrue(c.transactions.ready());
            c.transactions.begin();
            c.channel.writeOutbound(new C0EPacketClickWindow(7, 0, 0, 1, null, (short) 24));
            c.transactions.seal();
            c.channel.runPendingTasks();
            c.channel.writeInbound(new S32PacketConfirmTransaction(7, (short) 24, false));
            assertFalse(c.transactions.rejected()); // rejection is not acted upon before vanilla sees it
            c.network.processReceivedPackets();
            assertTrue(c.transactions.rejected());
            assertFalse(c.transactions.ready());
            assertEquals(1, c.requests);
        } finally {
            c.close();
        }
    }

    static void closingObserverDoesNotDiscardQueuedPackets() {
        Connection c = new Connection();
        c.channel.writeInbound(new S32PacketConfirmTransaction(7, (short) 23, false));
        c.channel.writeInbound(new S2FPacketSetSlot(7, 0, null));
        c.transactions.close();
        c.network.processReceivedPackets();
        assertEquals(1, c.confirmationsApplied);
        assertEquals(1, c.slotsApplied);
        assertFalse(c.transactions.ready());
        assertEquals(0, c.requests);
        c.channel.finish();
    }

    /** Only rendering is replaced. Container closing and packet handlers are the real Minecraft code. */
    private static final class HeadlessMinecraft extends Minecraft {

        private HeadlessMinecraft() {
            super(null, 0, 0, false, false, null, null, null, null, "", null, "");
        }

        @Override
        public void displayGuiScreen(GuiScreen screen) {
            currentScreen = screen;
            if (screen != null) screen.mc = this;
            if (screen instanceof GuiContainer) thePlayer.openContainer = ((GuiContainer) screen).inventorySlots;
        }
    }

    private static final class HeadlessPlayer extends EntityClientPlayerMP {

        private HeadlessPlayer() {
            super(null, null, null, null, null);
        }

        @Override
        public void addChatMessage(net.minecraft.util.IChatComponent message) {}
    }

    private static void setField(Class<?> type, Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class Client implements AutoCloseable {

        final NetworkManager network = new NetworkManager(true);
        final EmbeddedChannel channel = new EmbeddedChannel(network);
        final java.util.List<net.minecraft.network.Packet> sent = new java.util.ArrayList<>();
        final Minecraft previous = Minecraft.getMinecraft();
        final HeadlessMinecraft mc;

        Client() throws Exception {
            java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
            mc = (HeadlessMinecraft) unsafe.allocateInstance(HeadlessMinecraft.class);
            mc.thePlayer = (HeadlessPlayer) unsafe.allocateInstance(HeadlessPlayer.class);
            mc.thePlayer.inventory = new net.minecraft.entity.player.InventoryPlayer(mc.thePlayer);
            mc.thePlayer.inventoryContainer = container(0);
            mc.thePlayer.openContainer = mc.thePlayer.inventoryContainer;
            net.minecraft.entity.DataWatcher watcher = new net.minecraft.entity.DataWatcher(mc.thePlayer);
            watcher.addObject(6, 20.0F);
            setField(net.minecraft.entity.Entity.class, mc.thePlayer, "dataWatcher", watcher);
            setField(net.minecraft.client.entity.EntityPlayerSP.class, mc.thePlayer, "mc", mc);
            net.minecraft.client.network.NetHandlerPlayClient handler = new net.minecraft.client.network.NetHandlerPlayClient(
                mc,
                null,
                network) {

                @Override
                public void addToSendQueue(net.minecraft.network.Packet packet) {
                    sent.add(packet);
                    channel.writeOutbound(packet);
                }
            };
            setField(EntityClientPlayerMP.class, mc.thePlayer, "sendQueue", handler);
            network.setNetHandler(handler);
            setField(Minecraft.class, null, "theMinecraft", mc);
        }

        Container container(int window) {
            Container container = new Container() {

                @Override
                public boolean canInteractWith(EntityPlayer player) {
                    return true;
                }
            };
            container.windowId = window;
            return container;
        }

        GuiContainer gui(Container container) {
            GuiContainer gui = new GuiContainer(container) {

                @Override
                protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {}
            };
            gui.mc = mc;
            return gui;
        }

        WorklistScreen worklist(GuiContainer gui) throws Exception {
            WorklistScreen owner = new WorklistScreen(gui, null);
            owner.mc = mc;
            return owner;
        }

        @Override
        public void close() throws Exception {
            mc.thePlayer = null;
            CraftingSession.tick();
            channel.finish();
            setField(Minecraft.class, null, "theMinecraft", previous);
        }
    }

    static void worldAndCommandEntryCloseOrphanedContainer() throws Exception {
        try (Client c = new Client()) {
            c.mc.thePlayer.openContainer = c.container(7); // e.g. an old backpack window
            GuiContainer inventory = ClientProxy.openPlayerInventory(c.mc);
            assertTrue(inventory instanceof net.minecraft.client.gui.inventory.GuiInventory);
            assertSame(c.mc.thePlayer.inventoryContainer, inventory.inventorySlots);
            assertSame(inventory, c.mc.currentScreen);
            assertEquals(1, c.sent.size());
            assertTrue(c.sent.get(0) instanceof net.minecraft.network.play.client.C0DPacketCloseWindow);

            c.mc.currentScreen = new net.minecraft.client.gui.GuiChat();
            Container orphan = c.container(8);
            c.mc.thePlayer.openContainer = orphan;
            ClientProxy.queueCommand(null);
            assertSame(orphan, c.mc.thePlayer.openContainer); // queuing must not initialize a GuiInventory
            assertEquals(1, c.sent.size());
            c.mc.currentScreen = null; // GuiChat closes before END tick
            new ClientProxy().tick(
                new cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent(
                    cpw.mods.fml.common.gameevent.TickEvent.Phase.END));
            assertEquals(2, c.sent.size());
            assertTrue(c.sent.get(1) instanceof net.minecraft.network.play.client.C0DPacketCloseWindow);
            assertTrue(c.mc.currentScreen instanceof GroupScreen);
            assertTrue(
                ((WorklistGui) c.mc.currentScreen)
                    .rootContainer() instanceof net.minecraft.client.gui.inventory.GuiInventory);
            assertSame(c.mc.thePlayer.inventoryContainer, c.mc.thePlayer.openContainer);
        }
    }

    static void staleWorklistClosesServerWindowAndCannotCraft() throws Exception {
        try (Client c = new Client()) {
            GuiContainer original = c.gui(c.container(7));
            WorklistScreen owner = c.worklist(original);
            Container replacement = c.container(7);
            c.mc.thePlayer.openContainer = replacement;
            c.mc.currentScreen = owner;
            codechicken.nei.NEIClientConfig.hasSMPCounterpart = true;
            CraftingSession.start(owner, original, null, null, 1);
            assertFalse(CraftingSession.running());
            assertSame(replacement, c.mc.thePlayer.openContainer);
            assertSame(owner, c.mc.currentScreen);
            assertTrue(c.sent.isEmpty());
            owner.closeWorklist();
            assertNull(c.mc.currentScreen);
            assertSame(c.mc.thePlayer.inventoryContainer, c.mc.thePlayer.openContainer);
            assertEquals(1, c.sent.size());
            assertTrue(c.sent.get(0) instanceof net.minecraft.network.play.client.C0DPacketCloseWindow);
        }
    }

    static void sessionTimeoutAndContainerChangeStopWithoutClicks() throws Exception {
        try (Client c = new Client()) {
            codechicken.nei.NEIClientConfig.hasSMPCounterpart = true;
            GuiContainer original = c.gui(c.container(7));
            WorklistScreen owner = c.worklist(original);
            c.mc.thePlayer.openContainer = original.inventorySlots;
            c.mc.currentScreen = owner;
            CraftingSession.start(owner, original, null, null, 1);
            assertTrue(CraftingSession.running());
            assertSame(original, c.mc.currentScreen);
            CraftingTransactions observer = (CraftingTransactions) c.channel.pipeline()
                .get("worklist_transactions");
            int[] requests = { 0 };
            setField(CraftingTransactions.class, observer, "requestSnapshot", (Runnable) () -> requests[0]++);
            c.channel.runPendingTasks();
            for (int i = 0; i < CraftingTransactions.MAX_TICKS; i++) {
                CraftingSession.tick();
                assertTrue(CraftingSession.running());
            }
            CraftingSession.tick();
            assertFalse(CraftingSession.running());
            assertEquals(1, requests[0]);
            assertSame(original, c.mc.currentScreen); // timeout leaves real contents visible
            assertNull(
                c.channel.pipeline()
                    .get("worklist_transactions"));
            assertTrue(c.sent.isEmpty());

            c.mc.currentScreen = owner;
            CraftingSession.start(owner, original, null, null, 1);
            assertTrue(CraftingSession.running());
            c.mc.thePlayer.closeScreen(); // actual Escape behavior
            CraftingSession.tick();
            assertFalse(CraftingSession.running());
            assertNull(
                c.channel.pipeline()
                    .get("worklist_transactions"));
            assertEquals(1, c.sent.size());
            assertTrue(c.sent.get(0) instanceof net.minecraft.network.play.client.C0DPacketCloseWindow);
        }
    }
}
