package com.thunder.worldiconupdater;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.nio.file.Path;

@Mod("worldiconupdater")
public class WorldIconUpdater {
    public static final String MOD_ID = "worldiconupdater";
    private static boolean takingScreenshot = false;
    private static boolean screenshotPending = false;

    public WorldIconUpdater(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
    }

    public static boolean isScreenshotPending() {
        return screenshotPending;
    }

    public static void setScreenshotPending(boolean screenshotPending) {
        WorldIconUpdater.screenshotPending = screenshotPending;
    }

    @SubscribeEvent
    public void onScreenOpen(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isLocalServer() && minecraft.screen == null && event.getScreen() != null && event.getScreen().getClass().getSimpleName().contains("Pause")) {
            screenshotPending = true;
            takingScreenshot = true;
            this.takeWorldScreenshot();
        }

    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Pre event) {
        if (takingScreenshot) {
            event.setCanceled(true);
        }

    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (takingScreenshot && event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            takingScreenshot = false;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.isLocalServer()) {
                IntegratedServer server = minecraft.getSingleplayerServer();
                if (server != null && !server.isStopped()) {
                    server.getWorldScreenshotFile().ifPresent(this::captureCleanScreenshot);
                }
            }
        }

    }

    private void takeWorldScreenshot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.levelRenderer.countRenderedSections() > 10 && minecraft.levelRenderer.hasRenderedAllSections()) {
            screenshotPending = true;
        }

    }

    private void captureCleanScreenshot(Path path) {
        NativeImage nativeimage = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
        Util.ioPool().execute(() -> {
            int i = nativeimage.getWidth();
            int j = nativeimage.getHeight();
            int k = 0;
            int l = 0;
            if (i > j) {
                k = (i - j) / 2;
                i = j;
            } else {
                l = (j - i) / 2;
                j = i;
            }

            try {
                NativeImage nativeimage1 = new NativeImage(64, 64, false);

                try {
                    nativeimage.resizeSubRectTo(k, l, i, j, nativeimage1);
                    nativeimage1.writeToFile(path);
                } catch (Throwable var15) {
                    try {
                        nativeimage1.close();
                    } catch (Throwable var14) {
                        var15.addSuppressed(var14);
                    }

                    throw var15;
                }

                nativeimage1.close();
            } catch (IOException ioexception) {
                System.err.println("Couldn't save world screenshot: " + ioexception.getMessage());
            } finally {
                nativeimage.close();
            }

        });
    }
}
