package mirror.blahajasm.common.crashes;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mirror.blahajasm.config.BlahajConfig;

@SideOnly(Side.CLIENT)
public class GuiCrashScreen extends GuiProblemScreen {

    public GuiCrashScreen(CrashReport report) {
        super(report);
    }

    @Override
    public void initGui() {
        super.initGui();
        GuiOptionButton mainMenuButton = new GuiOptionButton(0, width / 2 - 155, height / 4 + 120 + 12, I18n.format("gui.toTitle"));
        buttonList.add(mainMenuButton);
        if (!BlahajConfig.instance.returnToMainMenuAfterCrash) {
            mainMenuButton.enabled = false;
            mainMenuButton.displayString = I18n.format("blahajasm.gui.disabledByConfig");
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);
        if (button.id == 0) {
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) { // TODO: localize number of lines
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("blahajasm.crashscreen.title"), width / 2, height / 4 - 40, 0xFFFFFF);

        int textColor = 0xD0D0D0;
        int x = width / 2 - 155;
        int y = height / 4;

        if (BlahajConfig.instance.crashReportUpdatedScreens) {
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.summary"), x, y -= 18, textColor);

            drawCenteredString(fontRenderer, "\u00A7n" + I18n.format("blahajasm.crashscreen.new.paragraph1.line1"), width / 2, y += 18, 0xFF0000);

            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.new.paragraph2.line1"), x, y += 18, textColor);
            drawCenteredString(fontRenderer, getModListString(), width / 2, y += 11, 0xE0E000);

            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.new.paragraph3.line1"), x, y += 11, textColor);
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.new.paragraph3.line2"), x, y += 9, textColor);

            drawCenteredString(fontRenderer, report.getFile() != null ?
                    "\u00A7n" + report.getFile().getName() :
                    I18n.format("blahajasm.crashscreen.reportSaveFailed"), width / 2, y += 11, 0x00FF00);

            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.new.paragraph4.line1"), x, y += 12, textColor);
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.new.paragraph4.line2"), x, y += 9, textColor);
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.new.paragraph4.line3"), x, y += 9, textColor);
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.new.paragraph4.line4"), x, y + 9, textColor);
        }
        else {
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.summary"), x, y, textColor);
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.paragraph1.line1"), x, y += 18, textColor);

            drawCenteredString(fontRenderer, getModListString(), width / 2, y += 11, 0xE0E000);

            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.paragraph2.line1"), x, y += 11, textColor);
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.paragraph2.line2"), x, y += 9, textColor);

            drawCenteredString(fontRenderer, report.getFile() != null ?
                    "\u00A7n" + report.getFile().getName() :
                    I18n.format("blahajasm.crashscreen.reportSaveFailed"), width / 2, y += 11, 0x00FF00);

            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.paragraph3.line1"), x, y += 12, textColor);
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.paragraph3.line2"), x, y += 9, textColor);
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.paragraph3.line3"), x, y += 9, textColor);
            drawString(fontRenderer, I18n.format("blahajasm.crashscreen.paragraph3.line4"), x, y + 9, textColor);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}