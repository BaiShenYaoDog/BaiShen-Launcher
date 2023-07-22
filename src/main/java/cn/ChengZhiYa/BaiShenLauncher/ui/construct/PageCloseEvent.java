package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Indicates a close operation on the navigator page. 
 */
public class PageCloseEvent extends Event {

    public static final EventType<PageCloseEvent> CLOSE = new EventType<>("PAGE_CLOSE");

    public PageCloseEvent() {
        super(CLOSE);
    }

    public PageCloseEvent(Object source, EventTarget target) {
        super(source, target, CLOSE);
    }

}
