package net.creeperhost.minetogether.client.gui;

import net.creeperhost.minetogether.MineTogether;
import net.creeperhost.minetogether.util.Util;
import net.creeperhost.minetogether.api.Order;
import net.creeperhost.minetogether.paul.Callbacks;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.multiplayer.ServerList;

import java.net.URI;
import java.util.List;

/**
 * Created by Aaron on 02/05/2017.
 */
public class GuiOrderDetails extends GuiGetServer
{
    private boolean placingOrder = false;
    private boolean placedOrder = false;
    private boolean creatingAccount = false;
    private boolean createdAccount = false;
    private String createdAccountError = "";
    private int orderNumber;
    private String invoiceID;
    private String placedOrderError = "";
    private Button buttonInvoice;
    private boolean serverAdded;
    
    
    public GuiOrderDetails(int stepId, Order order)
    {
        super(stepId, order);
        if (order.clientID != null && !order.clientID.isEmpty())
        {
            creatingAccount = false;
            createdAccount = true;
        }
    }
    
    @Override
    public String getStepName()
    {
        return Util.localize("gui.order");
    }
    
    @SuppressWarnings("Duplicates")
    @Override
    public void init()
    {
        super.init();
        this.buttonNext.visible = false;
        buttonCancel.setMessage(Util.localize("order.ordercancel"));
        buttonCancel.active = false;
        buttonInvoice = addButton(new Button( this.width / 2 - 40, (this.height / 2) + 30, 80, 20, Util.localize("button.invoice"), p ->
        {
            try
            {
                Class<?> oclass = Class.forName("java.awt.Desktop");
                Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object) null, new Object[0]);
                oclass.getMethod("browse", new Class[]{URI.class}).invoke(object, new Object[]{new URI(MineTogether.instance.getImplementation().getPaymentLink(invoiceID))});
            } catch (Throwable throwable)
            {
                MineTogether.logger.error("Couldn\'t open link", throwable);
            }
        }));
        buttonInvoice.visible = false;
    }

    @SuppressWarnings("Duplicates")
    public void tick()
    {
        super.tick();
        if (!createdAccount && !creatingAccount)
        {
            if (!createdAccountError.isEmpty())
            {
                buttonCancel.active = true;
                return;
            }
            creatingAccount = true;
            Runnable runnable = () -> {
                String result = Callbacks.createAccount(order);
                String[] resultSplit = result.split(":");
                if (resultSplit[0].equals("success"))
                {
                    order.currency = resultSplit[1] != null ? resultSplit[1] : "1";
                    order.clientID = resultSplit[2] != null ? resultSplit[2] : "0"; // random test account fallback

                } else
                {
                    createdAccountError = result;
                    createdAccount = true;
                }
                creatingAccount = false;
                createdAccount = true;
            };
            Thread thread = new Thread(runnable);
            thread.start();
        } else if (creatingAccount)
        {
            return;
        } else if (!createdAccountError.isEmpty())
        {
            buttonCancel.active = true;
            return;
        } else if (!placingOrder && !placedOrder)
        {
            placingOrder = true;
            buttonNext.active = false;
            Runnable runnable = () -> {
                String result = Callbacks.createOrder(order);
                String[] resultSplit = result.split(":");
                if (resultSplit[0].equals("success"))
                {
                    invoiceID = resultSplit[1] != null ? resultSplit[1] : "0";
                    orderNumber = Integer.valueOf(resultSplit[2]);
                } else
                {
                    placedOrderError = result;
                }
                placedOrder = true;
                placingOrder = false;
            };
            Thread thread = new Thread(runnable);
            thread.start();
        } else if (placingOrder)
        {
            return;
        } else if (placedOrderError.isEmpty())
        {
            if (!serverAdded)
            {
                ServerList savedServerList = new ServerList(this.minecraft);
                savedServerList.loadServerList();
                savedServerList.addServerData(MineTogether.instance.getImplementation().getServerEntry(order));
                savedServerList.saveServerList();
                serverAdded = true;
            }
            buttonInvoice.visible = true;
            buttonNext.visible = true;
            buttonCancel.active = true;
            return;
        } else {
            buttonNext.active = true;
        }
    }
    
    @SuppressWarnings("Duplicates")
    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        this.renderDirtBackground(0);
        if (creatingAccount)
        {
            drawCenteredString(font, Util.localize("order.accountcreating"), this.width / 2, this.height / 2, 0xFFFFFF);
        } else if (!createdAccountError.isEmpty())
        {
            drawCenteredString(font, Util.localize("order.accounterror"), this.width / 2, this.height / 2, 0xFFFFFF);
            List<String> list = font.listFormattedStringToWidth(createdAccountError, width - 30);
            int offset = 10;
            for (String str : list)
            {
                drawCenteredString(font, str, this.width / 2, (this.height / 2) + offset, 0xFFFFFF);
                offset += 10;
            }
            drawCenteredString(font, Util.localize("order.accounterrorgoback"), this.width / 2, this.height / 2 + offset, 0xFFFFFF);
        } else if (placingOrder)
        {
            drawCenteredString(font, Util.localize("order.orderplacing"), this.width / 2, this.height / 2, 0xFFFFFF);
        } else if (!placedOrderError.isEmpty())
        {
            drawCenteredString(font, Util.localize("order.ordererror"), this.width / 2, this.height / 2, 0xFFFFFF);
            List<String> list = font.listFormattedStringToWidth(placedOrderError, width - 30);
            int offset = 10;
            for (String str : list)
            {
                drawCenteredString(font, str, this.width / 2, (this.height / 2) + offset, 0xFFFFFF);
                offset += 10;
            }
            drawCenteredString(font, Util.localize("order.ordererrorsupport"), this.width / 2, (this.height / 2) + offset, 0xFFFFFF);
        } else
        {
            drawCenteredString(font, Util.localize("order.ordersuccess"), this.width / 2, this.height / 2, 0xFFFFFF);
            drawCenteredString(font, Util.localize("order.ordermodpack"), (this.width / 2) + 10, (this.height / 2) + 10, 0xFFFFFF);
        }
        super.render(mouseX, mouseY, partialTicks);
    }
}