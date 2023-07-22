package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.layout.Region;

/**
 * Indicates a close operation on the dialog.
 *
 * @author yushijinhun
 * @see Controllers#dialog(Region)
 */
public class DialogCloseEvent extends Event {

    public static final EventType<DialogCloseEvent> CLOSE = new EventType<>("DIALOG_CLOSE");

    public DialogCloseEvent() {
        super(CLOSE);
    }

    public DialogCloseEvent(Object source, EventTarget target) {
        super(source, target, CLOSE);
    }

}
