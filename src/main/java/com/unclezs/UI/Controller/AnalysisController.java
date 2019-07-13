package com.unclezs.UI.Controller;

import com.jfoenix.controls.*;
import com.unclezs.Crawl.NovelSpider;
import com.unclezs.Downloader.NovelDownloader;
import com.unclezs.Mapper.AnalysisMapper;
import com.unclezs.Mapper.ChapterMapper;
import com.unclezs.Mapper.NovelMapper;
import com.unclezs.Mapper.SettingMapper;
import com.unclezs.Model.AnalysisConfig;
import com.unclezs.Model.Book;
import com.unclezs.Model.Chapter;
import com.unclezs.Model.DownloadConfig;
import com.unclezs.UI.Node.ProgressFrom;
import com.unclezs.UI.Utils.AlertUtil;
import com.unclezs.UI.Utils.DataManager;
import com.unclezs.UI.Utils.LayoutUitl;
import com.unclezs.UI.Utils.ToastUtil;
import com.unclezs.Utils.FileUtil;
import com.unclezs.Utils.MybatisUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.apache.ibatis.session.SqlSession;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/*
 *解析下载页面
 *@author unclezs.com
 *@date 2019.06.26 16:44
 */
public class AnalysisController implements Initializable {

    //解析设置面板配置
    @FXML
    VBox menuPane;//菜单面板
    @FXML
    TextArea chapterHeadText, cookiesText, UAText, AdText, chapterTailText, contentHeadText, contentTailText;
    @FXML
    JFXRadioButton chapterFilterUse, chapterSortUse, NCRToXZhUse, TraToSimpleUse, rule1, rule2, rule3, startDynamic;
    //其他
    @FXML
    JFXTextField text;//输入框
    @FXML
    JFXListView<JFXCheckBox> list;//章节listView
    @FXML
    JFXTextArea content;//解析正文
    @FXML
    Label menu;//菜单
    @FXML
    Pane analysisRoot, doPane;//根容器
    @FXML
    JFXButton saveConfigBtn, analysisBtn, addToMark, downloadIt;//解析、加入书架按钮

    //成员
    ContextMenu contextMenu = new ContextMenu();
    private ObservableList<String> chapterUrlList;//url列表
    private Map<String, String> chapterMap;//url列表
    private AnalysisConfig config = new AnalysisConfig();//解析配置
    private NovelSpider spider;//爬虫
    private boolean startSelected = false;//shift多选开启标志
    private int startIndex;//多选开始位置

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        init();
    }

    void init() {
        //绑定匹配规则单选
        ToggleGroup group = new ToggleGroup();
        rule1.setToggleGroup(group);
        rule2.setToggleGroup(group);
        rule3.setToggleGroup(group);
        //自适应
        autoSize();
        saveCongfig();//加载解析配置
        initContextMenu();//右键菜单
        //自动导入剪贴板内容到输入框
        if (DataManager.content.getChildren().size() > 0 && DataManager.content.getChildren().get(0).getId().equals("searchRoot")) {
            autoImportClibord(true);
        }
        //解析章节
        analysisBtn.setOnMouseClicked(e -> {
            //防空
            String url = this.text.getText();
            if ("".equals(url) || url.equals(null) || !url.startsWith("http")) {
                ToastUtil.toast("请先输入文章目录地址");
                return;
            }
            analysisChapter();
        });
        //双击章节显示章节内容
        list.setOnMouseClicked(event -> {
            if (event.isShiftDown()) {//shift多选
                if (!startSelected) {
                    startSelected = true;
                    startIndex = list.getSelectionModel().getSelectedIndex();
                    list.getItems().get(startIndex).setSelected(!list.getItems().get(startIndex).isSelected());
                    return;
                }
                if (startSelected) {
                    int end = list.getSelectionModel().getSelectedIndex();
                    System.out.println(startIndex + "--" + end);
                    for (int i = startIndex + 1; i <= end; i++) {
                        list.getItems().get(i).setSelected(!list.getItems().get(i).isSelected());
                    }
                    startSelected = false;
                }
            } else {//非多选请求
                if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY && list.getItems().size() > 0) {
                    int index = list.getSelectionModel().getSelectedIndex();
                    showChapterContent(index);
                }
                if (event.getButton() == MouseButton.SECONDARY && list.getItems().size() > 0) {
                    contextMenu.setY(event.getScreenY());
                    contextMenu.setX(event.getScreenX());
                    contextMenu.getItems().get(2).setOnAction(e -> {//全选
                        for (JFXCheckBox cb : list.getItems()) {
                            cb.setSelected(true);
                        }
                    });
                    contextMenu.getItems().get(3).setOnAction(e -> {//全不选
                        for (JFXCheckBox cb : list.getItems()) {
                            cb.setSelected(false);
                        }
                    });
                    contextMenu.getItems().get(0).setOnAction(e -> {
                        showChapterContent(list.getSelectionModel().getSelectedIndex());
                    });
                    contextMenu.show(DataManager.mainStage);
                }
            }
        });
        //添加到书架
        addToMark.setOnMouseClicked(e -> {
            addToBookSelf();
        });
        //菜单按钮
        menu.setOnMouseClicked(e -> {
            menuPane.setVisible(!menuPane.isVisible());
        });
        //保存解析配置
        saveConfigBtn.setOnMouseClicked(e -> {
            saveCongfig();
            menuPane.setVisible(false);
            ToastUtil.toast("保存成功");
        });
        //点击输入框自动导入剪贴板内容
        text.setOnMouseClicked(e -> {
            autoImportClibord(false);
            //光标移动到末尾
            text.selectEnd();
            text.deselect();
        });
        //下载
        downloadIt.setOnMouseClicked(e -> {
            downloadBook();
        });
    }

    //显示章节内容
    private void showChapterContent(int index) {
        content.setText("正在获取章节：" + chapterMap.get(chapterUrlList.get(index)));
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                Map<String, String> config = spider.getConfig();
                String text = spider.getContent(chapterUrlList.get(index), config.get("charset"));
                return text;
            }
        };
        new Thread(task).start();
        task.setOnSucceeded(e -> {
            String text = task.getValue();
            if (text == null || "".equals(text)) {
                ToastUtil.toast("没有匹配到正文，可以换个匹配规则试试！");
                content.setText("没有匹配到正文，可以换个匹配规则试试！");
            } else {
                content.setText(text);
            }
        });
    }

    //解析章节目录
    private void analysisChapter() {
        String url = this.text.getText();
        //防止空指针
        if ("".equals(url) || url.equals(null) || !url.startsWith("http")) {
            return;
        }
        Platform.runLater(() -> {
            list.getItems().clear(); //清除原有的
            Task task = new Task() {
                @Override
                protected Object call() throws Exception {
                    //爬取章节列表
                    chapterMap = spider.getChapterList(url);
                    chapterUrlList = FXCollections.observableArrayList();
                    for (String c : chapterMap.keySet()) {
                        chapterUrlList.add(c);
                    }
                    return null;
                }
            };
            new Thread(task).start();
            ProgressFrom pf = new ProgressFrom(DataManager.mainStage);
            task.setOnSucceeded(e -> {
                //加入listView
                for (String c : chapterMap.keySet()) {
                    JFXCheckBox cb = new JFXCheckBox();
                    cb.setText(chapterMap.get(c));
                    cb.setSelected(true);
                    list.getItems().add(cb);
                }
                pf.cancelProgressBar();
            });
            pf.activateProgressBar();
        });
    }

    //初始化右键菜单
    private void initContextMenu() {
        MenuItem selectAll = new MenuItem("全选");
        MenuItem showContent = new MenuItem("查看内容");
        MenuItem unSelectAll = new MenuItem("全不选");
        selectAll.setGraphic(new ImageView("images/解析页/全选.jpg"));
        unSelectAll.setGraphic(new ImageView("images/解析页/反选.jpg"));
        showContent.setGraphic(new ImageView("images/解析页/查看.jpg"));
        contextMenu.getItems().addAll(showContent, new SeparatorMenuItem(), selectAll, unSelectAll);
    }

    //添加到书架
    private void addToBookSelf() {
        if (chapterUrlList == null || chapterUrlList.size() == 0) {
            ToastUtil.toast("请先解析目录后再添加！");
            return;
        }
        List<String> selectedNameItems = new ArrayList<>();//选中的章节名字列表
        //赛选出选中的条目
        for (JFXCheckBox cb : list.getItems()) {
            if (cb.isSelected()) {
                selectedNameItems.add(cb.getText());
            }
        }
        //loading
        Task task = new Task() {
            @Override
            protected Object call() throws Exception {
                Map<String, String> config = spider.getConfig();
                String name = config.get("title");
                String charset = config.get("charset");
                String homeUrl = text.getText();
                //封面下载
                String imgUrl = spider.crawlDescImage(name);
                String imgPath = FileUtil.uploadFile("./image/" + name + ".jpg", imgUrl);
                //保存书籍信息
                Book book = new Book(name, homeUrl, imgPath);
                book.setCharset(charset);
                book.setIsWeb(1);//标记为网络书籍
                SqlSession sqlSession = MybatisUtils.openSqlSession(true);//开启sqlSession
                NovelMapper mapper = sqlSession.getMapper(NovelMapper.class);
                mapper.save(book);
                //保存选中的章节信息
                Integer id = mapper.findLastOne().getId();
                List<Chapter> chapters = new ArrayList<>();
                for (String c : chapterMap.keySet()) {
                    if (selectedNameItems.contains(chapterMap.get(c))) {
                        chapters.add(new Chapter(chapterMap.get(c), c, id));
                    }
                }
                sqlSession.getMapper(ChapterMapper.class).saveChapters(chapters);//保存章节信息入库
                sqlSession.getMapper(AnalysisMapper.class).saveAnalysisConfig(spider.getConf(), id);//保存解析器配置
                sqlSession.close();
                DataManager.needReloadBookSelf = true;//刷新书架
                return null;
            }
        };
        new Thread(task).start();
        ProgressFrom pf = new ProgressFrom(DataManager.mainStage);
        task.setOnSucceeded(e -> {
            pf.cancelProgressBar();
            ToastUtil.toast("添加成功!");
        });
        pf.activateProgressBar();
    }

    //保存配置
    private void saveCongfig() {
        config.setAdStr(AdText.getText());
        config.setCookies(cookiesText.getText());
        config.setChapterFilter(chapterFilterUse.isSelected());
        config.setChapterHead(chapterHeadText.getText());
        config.setChapterSort(chapterSortUse.isSelected());
        config.setChapterTail(chapterTailText.getText());
        config.setContentHead(contentHeadText.getText());
        config.setContentTail(contentTailText.getText());
        config.setNcrToZh(NCRToXZhUse.isSelected());
        config.setTraToSimple(TraToSimpleUse.isSelected());
        if (rule3.isSelected()) {
            config.setRule("3");
        } else {
            config.setRule(rule1.isSelected() ? "1" : "2");
        }
        config.setStartDynamic(startDynamic.isSelected());
        config.setUserAgent(UAText.getText());
        if (spider == null) {
            spider = new NovelSpider(config);
        } else {
            spider.setConf(config);//添加到爬虫配置
        }
    }

    //自适应
    private void autoSize() {
        LayoutUitl.bind(DataManager.root, analysisRoot);
        list.prefWidthProperty().bind(analysisRoot.widthProperty().divide(2).subtract(10));
        content.prefWidthProperty().bind(list.prefWidthProperty().subtract(25));
        content.layoutXProperty().bind(list.layoutXProperty().add(list.widthProperty()).add(20));
        content.prefHeightProperty().bind(analysisRoot.heightProperty().subtract(100));
        list.prefHeightProperty().bind(content.heightProperty());
        menu.layoutXProperty().bind(analysisRoot.layoutXProperty().add(analysisRoot.widthProperty()).subtract(menu.widthProperty()).subtract(10));
        menuPane.layoutXProperty().bind(analysisRoot.layoutXProperty().add(analysisRoot.widthProperty()).subtract(menuPane.widthProperty()));
        doPane.layoutXProperty().bind(analysisRoot.layoutXProperty().add(analysisRoot.widthProperty().divide(2)).subtract(doPane.widthProperty().divide(2)));
    }

    //自动导入剪贴班链接
    void autoImportClibord(boolean isAnalysis) {
        Clipboard cb = Clipboard.getSystemClipboard();
        String url = cb.getString();
        if (url != null && !"".equals(url) && url.startsWith("http")) {
            text.setText(url);
            if (isAnalysis) {//如果需要解析则，导入自动后解析
                analysisChapter();
            }
        }
    }

    //下载本书
    private void downloadBook() {
        if (list.getItems().size() == 0) {
            ToastUtil.toast("请先解析目录！");
            return;
        }
        SettingMapper mapper = MybatisUtils.getMapper(SettingMapper.class);
        DownloadConfig config = mapper.querySetting();
        if ("".equals(config.getPath()) || config.getPath() == null) {//路径不为空的时候使用当前路径
            config.setPath(new File("./").getAbsolutePath().replace(".",""));
            mapper.updateSetting(config);
        } else if (!new File(config.getPath()).exists()) {
            ToastUtil.toast("保存路径不存在！");
            return;
        }
        //赛选出选中的条目
        List<String> taskUrlList = new ArrayList<>();
        List<String> selectedNameItems = new ArrayList<>();
        for (JFXCheckBox cb : list.getItems()) {
            if (cb.isSelected()) {
                selectedNameItems.add(cb.getText());
            }
        }
        for (String c : chapterMap.keySet()) {
            if (selectedNameItems.contains(chapterMap.get(c))) {
                taskUrlList.add(c);
            }
        }
        NovelDownloader downloder = new NovelDownloader(taskUrlList, selectedNameItems, config, spider);
        DownloadController.addTask(downloder);
        new Thread(() -> {
            downloder.start();
        }).start();
        ToastUtil.toast("添加下载任务成功！");
    }
}
