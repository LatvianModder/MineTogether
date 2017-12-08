package net.creeperhost.creeperhost.gui.serverlist;

import net.creeperhost.creeperhost.CreeperHost;
import net.creeperhost.creeperhost.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.LanServerDetector;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.lang.reflect.Field;

public class GuiMultiplayerPublic extends GuiMultiplayer
{
    private boolean initialized;
    private GuiScreen parent;
    private GuiButton modeToggle;
    public boolean isPublic = true;

    public GuiMultiplayerPublic(GuiScreen parentScreen)
    {
        super(parentScreen);
        parent = parentScreen;
    }

    public GuiMultiplayerPublic(GuiScreen parentScreen, boolean isPublic)
    {
        this(parentScreen);
        this.isPublic = isPublic;
    }

    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        if (this.initialized)
        {
            this.ourServerListSelector.setDimensions(this.width, this.height, 32, this.height - 64);
        }
        else
        {
            this.initialized = true;
            setServerList(new ServerListPublic(this.mc, this));
            ourSavedServerList.loadServerList();
            setLanServerList(new LanServerDetector.LanServerList());

            try
            {
                setLanServerDetector(new LanServerDetector.ThreadLanServerFind(this.ourLanServerList));
                ourLanServerDetector.start();
            }
            catch (Exception exception)
            {
            }

            setServerListSelector(new ServerSelectionListPublic(this, this.mc, this.width, this.height, 32, this.height - 64, 36));
            ourServerListSelector.updateOnlineServers(this.ourSavedServerList);
        }

        this.createButtons();
    }

    @Override
    public boolean canMoveUp(ServerListEntryNormal p_175392_1_, int p_175392_2_)
    {
        return false;
    }

    @Override
    public boolean canMoveDown(ServerListEntryNormal p_175394_1_, int p_175394_2_)
    {
        return false;
    }

    @Override
    public void createButtons()
    {
        super.createButtons();
        for(GuiButton button: buttonList)
        {
            if (button.id != 0 && button.id != 1 && button.id != 3)
            {
                button.visible = false;
            }

            if (button.id == 1) // original connect button
            {
                button.displayString = I18n.format("selectServer.add");
            }

            if (button.id == 3) // original add button
            {
                button.displayString = I18n.format("selectServer.refresh");
            }
        }
        modeToggle = new GuiButton(80085, width - 5 - 100, 5, 100, 20, Util.localize(isPublic ? "multiplayer.button.public" : "multiplayer.button.private"));
        buttonList.add(modeToggle);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 3)
        {
            refresh();
            /*try
            {

                AnvilSaveConverter converter = (AnvilSaveConverter) Minecraft.getMinecraft().getSaveLoader();
                WorldSummary worldSummary = converter.getSaveList().get(0);

                System.out.println(worldSummary.getFileName() + " " + worldSummary.getDisplayName());

                File folder = new File(converter.savesDirectory, worldSummary.getFileName());
                File zip = new File(converter.savesDirectory, worldSummary.getFileName() + ".zip");

                ZipUtils.zipIt(zip.getPath(), folder.getPath());

                System.out.println(ZipUtils.uploadFile(zip));

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }*/ // comment out world zipping and uploading stuff for now, moar work on other features!
            return;
        } else if (button.id == modeToggle.id) {
            isPublic = !isPublic;
            button.displayString = Util.localize(isPublic ? "multiplayer.button.public" : "multiplayer.button.private");
            refresh();
        }
        super.actionPerformed(button);
    }

    private void refresh()
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiMultiplayerPublic(parent, isPublic));
    }

    @Override
    public void connectToSelected()
    {
        GuiListExtended.IGuiListEntry entry = this.ourServerListSelector.getSelected() < 0 ? null : this.ourServerListSelector.getListEntry(this.ourServerListSelector.getSelected());
        ServerList savedServerList = new ServerList(this.mc);
        savedServerList.loadServerList();
        savedServerList.addServerData(((ServerListEntryNormal)entry).getServerData());
        savedServerList.saveServerList();

        Minecraft mc = Minecraft.getMinecraft();
        if (parent instanceof GuiMultiplayer)
        {
            mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
            return;
        }

        mc.displayGuiScreen(parent);
    }

    @Override
    public void drawCenteredString(FontRenderer fontRendererIn, String text, int x, int y, int color)
    {
        if (text.equals(I18n.format("multiplayer.title")))
        {
            text = Util.localize("multiplayer.public");
        }
        super.drawCenteredString(fontRendererIn, text, x, y, color);
    }

    private ServerList ourSavedServerList = null;
    private static Field savedServerListField;
    private void setServerList(ServerList serverList)
    {
        ourSavedServerList = serverList;
        if (savedServerListField == null)
        {
            savedServerListField = ReflectionHelper.findField(GuiMultiplayer.class, "field_146804_i", "savedServerList");
            savedServerListField.setAccessible(true);
        }

        try
        {
            savedServerListField.set(this, serverList);
        }
        catch (IllegalAccessException e)
        {
            CreeperHost.logger.error("Unable to set server list", e);
        }
    }

    private LanServerDetector.ThreadLanServerFind ourLanServerDetector = null;
    private static Field lanServerDetectorField;
    private void setLanServerDetector(LanServerDetector.ThreadLanServerFind detector)
    {
        ourLanServerDetector = detector;
        if (lanServerDetectorField == null)
        {
            lanServerDetectorField = ReflectionHelper.findField(GuiMultiplayer.class, "field_146800_B", "lanServerDetector");
            lanServerDetectorField.setAccessible(true);
        }

        try
        {
            lanServerDetectorField.set(this, detector);
        }
        catch (IllegalAccessException e)
        {
            CreeperHost.logger.error("Unable to set server list", e);
        }
    }

    private LanServerDetector.LanServerList ourLanServerList = null;
    private static Field lanServerListField;
    private void setLanServerList(LanServerDetector.LanServerList detector)
    {
        ourLanServerList = detector;
        if (lanServerListField == null)
        {
            lanServerListField = ReflectionHelper.findField(GuiMultiplayer.class, "field_146799_A", "lanServerList");
            lanServerListField.setAccessible(true);
        }

        try
        {
            lanServerListField.set(this, detector);
        }
        catch (IllegalAccessException e)
        {
            CreeperHost.logger.error("Unable to set server list", e);
        }
    }

    private ServerSelectionList ourServerListSelector = null;
    private static Field serverListSelectorField;
    private void setServerListSelector(ServerSelectionList list)
    {
        ourServerListSelector = list;
        if (serverListSelectorField == null)
        {
            serverListSelectorField = ReflectionHelper.findField(GuiMultiplayer.class, "field_146803_h", "serverListSelector");
            serverListSelectorField.setAccessible(true);
        }

        try
        {
            serverListSelectorField.set(this, list);
        }
        catch (IllegalAccessException e)
        {
            CreeperHost.logger.error("Unable to set server list", e);
        }
    }
}