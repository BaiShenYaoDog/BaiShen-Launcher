 package cn.ChengZhiYa.BaiShenLauncher.ui.main;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.ComponentList;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.IconedTwoLineListItem;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AboutPage extends StackPane {

    public AboutPage() {
        ComponentList about = new ComponentList();
        {
            IconedTwoLineListItem launcher = new IconedTwoLineListItem();
            launcher.setImage(new Image("/assets/img/icon.png", 32, 32, false, true));
            launcher.setTitle("BaiShen-Launcher");
            launcher.setSubtitle(Metadata.VERSION);

            IconedTwoLineListItem author = new IconedTwoLineListItem();
            author.setImage(new Image("/assets/img/ChengZhi.png", 32, 32, false, true));
            author.setTitle("白神遥桌上の橙汁");
            author.setSubtitle("该启动器修改者");
            author.setExternalLink("https://space.bilibili.com/687901384");

            IconedTwoLineListItem HMCLauthor = new IconedTwoLineListItem();
            HMCLauthor.setImage(new Image("/assets/img/yellow_fish.png"));
            HMCLauthor.setTitle("huanghongxun");
            HMCLauthor.setSubtitle("HMCL原作者");
            HMCLauthor.setExternalLink("https://space.bilibili.com/1445341");

            about.getContent().setAll(launcher, author, HMCLauthor);
        }

        ComponentList thanks = new ComponentList();
        {
            IconedTwoLineListItem BaiShenYao = new IconedTwoLineListItem();
            BaiShenYao.setImage(new Image("/assets/img/BaiShenYao.jpg", 32, 32, false, true));
            BaiShenYao.setTitle("白神遥Haruka");
            BaiShenYao.setSubtitle("我老婆,也是糯米糍们的老婆");
            BaiShenYao.setExternalLink("https://space.bilibili.com/477332594/");

            IconedTwoLineListItem DebuLiu = new IconedTwoLineListItem();
            DebuLiu.setImage(new Image("/assets/img/DebuLiu.jpg", 32, 32, false, true));
            DebuLiu.setTitle("debuLIU-official");
            DebuLiu.setSubtitle("提供泳装豹OP图qwq");
            DebuLiu.setExternalLink("https://space.bilibili.com/105251872/");

            IconedTwoLineListItem yushijinhun = new IconedTwoLineListItem();
            yushijinhun.setImage(new Image("/assets/img/yushijinhun.png"));
            yushijinhun.setTitle("yushijinhun");
            yushijinhun.setSubtitle(I18n.i18n("about.thanks_to.yushijinhun.statement"));
            yushijinhun.setExternalLink("https://yushi.moe/");

            IconedTwoLineListItem bangbang93 = new IconedTwoLineListItem();
            bangbang93.setImage(new Image("/assets/img/bangbang93.png"));
            bangbang93.setTitle("bangbang93");
            bangbang93.setSubtitle(I18n.i18n("about.thanks_to.bangbang93.statement"));
            bangbang93.setExternalLink("https://bmclapi2.bangbang93.com/");

            IconedTwoLineListItem glavo = new IconedTwoLineListItem();
            glavo.setImage(new Image("/assets/img/glavo.png"));
            glavo.setTitle("Glavo");
            glavo.setSubtitle(I18n.i18n("about.thanks_to.glavo.statement"));
            glavo.setExternalLink("https://github.com/Glavo");

            IconedTwoLineListItem mcbbs = new IconedTwoLineListItem();
            mcbbs.setImage(new Image("/assets/img/chest.png"));
            mcbbs.setTitle(I18n.i18n("about.thanks_to.mcbbs"));
            mcbbs.setSubtitle(I18n.i18n("about.thanks_to.mcbbs.statement"));
            mcbbs.setExternalLink("https://www.mcbbs.net/");

            IconedTwoLineListItem mcmod = new IconedTwoLineListItem();
            mcmod.setImage(new Image("/assets/img/mcmod.png"));
            mcmod.setTitle(I18n.i18n("about.thanks_to.mcmod"));
            mcmod.setSubtitle(I18n.i18n("about.thanks_to.mcmod.statement"));
            mcmod.setExternalLink("https://www.mcmod.cn/");

            IconedTwoLineListItem users = new IconedTwoLineListItem();
            users.setImage(new Image("/assets/img/craft_table.png"));
            users.setTitle(I18n.i18n("about.thanks_to.users"));
            users.setSubtitle(I18n.i18n("about.thanks_to.users.statement"));
            users.setExternalLink("https://hmcl.huangyuhui.net/api/redirect/sponsor");

            thanks.getContent().setAll(BaiShenYao, DebuLiu, yushijinhun, bangbang93, glavo, mcbbs, mcmod, users);
        }


        ComponentList legal = new ComponentList();
        {
            IconedTwoLineListItem copyright = new IconedTwoLineListItem();
            copyright.setTitle(I18n.i18n("about.copyright"));
            copyright.setSubtitle(I18n.i18n("about.copyright.statement"));
            copyright.setExternalLink("https://hmcl.huangyuhui.net/about/");

            IconedTwoLineListItem claim = new IconedTwoLineListItem();
            claim.setTitle("用户协议");
            claim.setSubtitle("点击链接以查看全文");
            claim.setExternalLink(Metadata.EULA_URL);

            IconedTwoLineListItem openSource = new IconedTwoLineListItem();
            openSource.setTitle("开源 | github");
            openSource.setSubtitle("GPL v3(https://github.com/ChengZhiNB/BaiShen-Launcher)");
            openSource.setExternalLink("https://github.com/ChengZhiNB/BaiShen-Launcher");

            legal.getContent().setAll(copyright, claim, openSource);
        }

        VBox content = new VBox(16);
        content.setPadding(new Insets(10));
        content.getChildren().setAll(
                ComponentList.createComponentListTitle(I18n.i18n("about")),
                about,

                ComponentList.createComponentListTitle(I18n.i18n("about.thanks_to")),
                thanks,

                ComponentList.createComponentListTitle("法律声明"),
                legal
        );


        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        FXUtils.smoothScrolling(scrollPane);
        getChildren().setAll(scrollPane);
    }
}
