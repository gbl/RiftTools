package de.guntram.mcmod.rifttools;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;

public class GuiModOptions extends GuiScreen {
    
    private final GuiScreen parent;
    private final String modName;
    private final ModConfigurationHandler handler;
    private final List<String> options;
    
    private String screenTitle;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GuiModOptions(GuiScreen parent, String modName, ModConfigurationHandler confHandler) {
        this.parent=parent;
        this.modName=modName;
        this.handler=confHandler;
        this.screenTitle=modName+" Configuration";
        this.options=handler.getConfig().getKeys();
    }
    
    @Override
    protected void initGui() {
        this.addButton(new GuiButton(200, this.width / 2 - 100, this.height - 27, I18n.format("gui.done")) {
            @Override
            public void onClick(double x, double y) {
                handler.onConfigChanged(new ConfigChangedEvent.OnConfigChangedEvent(modName));
                mc.displayGuiScreen(parent);
            }
        });
        
        int y=30;
        for (String text: options) {
            y+=20;
            Object value = handler.getConfig().getValue(text);
            IGuiEventListener element;
            if (value == null) {
                continue;
            } else if (value instanceof Boolean) {
                element = this.addButton(new GuiButton(0, this.width/2+10, y, ((Boolean) value == true ? "true" : "false")) {
                    @Override
                    public void onClick(double x, double y) {
                        if ((Boolean)(handler.getConfig().getValue(text))==true) {
                            handler.getConfig().setValue(text, false);
                        } else {
                            handler.getConfig().setValue(text, true);
                        }
                        this.focusChanged(true);
                    }
                    @Override
                    public void focusChanged(boolean b) {
                        this.displayString=((Boolean) handler.getConfig().getValue(text) == true ? "true" : "false");
                        super.focusChanged(b);
                    }
                });
            } else if (value instanceof String) {
                element=new GuiTextField(0, this.fontRenderer, this.width/2+10, y, 200, 20) {
                    @Override
                    public void focusChanged(boolean b) {
                        this.setText((String) handler.getConfig().getValue(text));
                        super.focusChanged(b);
                    }
                };
                element.focusChanged(false);
                this.children.add(element);
            } else if (value instanceof Integer || value instanceof Float || value instanceof Double) {
                element=this.addButton(new GuiSlider(this.width/2+10, y, handler.getConfig(), text));
            } else {
                continue;
            }
            this.addButton(new GuiButton(0, this.width/2+220, y, 20, 20, "") {
                @Override
                public void onClick(double x, double y) {
                    handler.getConfig().setValue(text, handler.getConfig().getDefault(text));
                    element.focusChanged(false);
                }
            });
        }
    }
    
    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, screenTitle, this.width/2, 20, 0xffffff);
        super.render(mouseX, mouseY, partialTicks);
        
        int y=50;
        for (String text: options) {
            drawString(fontRenderer, text, this.width / 2 -155, y+2, 0xffffff);
            y+=20;
        }

        y=50;
        for (String text: options) {
            if (mouseX>this.width/2-155 && mouseX<this.width/2 && mouseY>y && mouseY<y+20) {
                String tooltip=handler.getConfig().getTooltip(text);
                if (tooltip==null)
                    tooltip="missing tooltip";
                if (tooltip.length()<=30) {
                    drawHoveringText(handler.getConfig().getTooltip(text), mouseX, mouseY);
                } else {
                    List<String>lines=new ArrayList<>();
                    int pos=0;
                    while (pos<tooltip.length()-30) {
                        int curlen=30;
                        while (tooltip.charAt(pos+curlen)!=' ' && curlen>10) {
                            curlen--;
                        }
                        lines.add(tooltip.substring(pos, pos+curlen));
                        pos+=curlen+1;
                    }
                    lines.add(tooltip.substring(pos));
                    drawHoveringText(lines, mouseX, mouseY);
                }
            }
            y+=20;
        }
    }
    
    private enum Type {INT, FLOAT, DOUBLE;}
    
    private class GuiSlider extends GuiButton {
        Type type;
        boolean dragging;
        double sliderValue, min, max;
        Configuration config;
        String configOption;
        
        GuiSlider(int x, int y, Configuration config, String option) {
            super(0, x, y, 200, 20, "?");
            Object value=config.getValue(option);
            if (value instanceof Double) {
                this.displayString=Double.toString((Double)value);
                this.min=(Double)config.getMin(option);
                this.max=(Double)config.getMax(option);
                sliderValue=((Double)value-min)/(max-min);
                type=Type.DOUBLE;
            }
            else if (value instanceof Float) {
                this.displayString=Float.toString((Float)value);
                this.min=(Float)config.getMin(option);
                this.max=(Float)config.getMax(option);
                sliderValue=((Float)value-min)/(max-min);
                type=Type.FLOAT;
            } else {
                this.displayString=Integer.toString((Integer)value);
                this.min=(Integer)config.getMin(option);
                this.max=(Integer)config.getMax(option);
                sliderValue=((Integer)value-min)/(max-min);
                type=Type.INT;
            }

            this.config=config;
            this.configOption=option;
        }
        
        private void updateValue(double value) {
            switch (type) {
                case DOUBLE:
                    double doubleVal=value*(max-min)+min;
                    this.displayString=Double.toString(doubleVal);
                    this.config.setValue(configOption, (Double) doubleVal);
                    break;
                case FLOAT:
                    float floatVal=(float) (value*(max-min)+min);
                    this.displayString=Float.toString(floatVal);
                    this.config.setValue(configOption, (Float) floatVal);
                    break;
                case INT:
                    int intVal=(int) (value*(max-min)+min);
                    this.displayString=Integer.toString(intVal);
                    this.config.setValue(configOption, (Integer) intVal);
                    break;
            }
        }

        @Override
        protected void renderBg(Minecraft mc, int mouseX, int mouseY)
        {
            if (this.visible)
            {
                if (this.dragging)
                {
                    this.sliderValue = (double)((float)(mouseX - (this.x + 4)) / (float)(this.width - 8));
                    this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
                    updateValue(this.sliderValue);
                }
                mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.drawTexturedModalRect(this.x + (int)(this.sliderValue * (double)(this.width - 8)), this.y, 0, 66, 4, 20);
                this.drawTexturedModalRect(this.x + (int)(this.sliderValue * (double)(this.width - 8)) + 4, this.y, 196, 66, 4, 20);
            }
        }

        /**
         * Called when the left mouse button is pressed over this button. This method is specific to GuiButton.
         */
        @Override
        public final void onClick(double mouseX, double mouseY)
        {
            this.sliderValue = (mouseX - (double)(this.x + 4)) / (double)(this.width - 8);
            this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
            updateValue(sliderValue);
            this.dragging = true;
        }

        /**
         * Called when the left mouse button is released. This method is specific to GuiButton.
         */
        @Override
        public void onRelease(double mouseX, double mouseY)
        {
            this.dragging = false;
        }
        
        @Override
        public void focusChanged(boolean b) {
            Object value=config.getValue(configOption);
            if (value instanceof Double) {
                this.displayString=Double.toString((Double)value);
                sliderValue=((Double)value-min)/(max-min);
            }
            else if (value instanceof Float) {
                this.displayString=Float.toString((Float)value);
                sliderValue=((Float)value-min)/(max-min);
            } else {
                this.displayString=Integer.toString((Integer)value);
                sliderValue=((Integer)value-min)/(max-min);
            }
            super.focusChanged(b);
        }
    }
}
