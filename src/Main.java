import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 类名 ClassName  Main
 * 项目 ProjectName  TxtFileSlicer
 * 作者 Author  郑添翼 Taky.Zheng
 * 邮箱 E-mail 275158188@qq.com
 * 时间 Date  2019-07-20 20:27 ＞ω＜
 * 描述 Description TODO
 */
public class Main extends Application {



    File srcFile;
    File tarFile;

    String prefixStr = "";

    Integer rowCount = 1000000;

    @Override
    public void start(Stage primaryStage) throws Exception {


        Label seleLable = new Label("请选择文件:");
        TextField seleTf = new TextField();
        seleTf.setEditable(false);
        seleTf.setPromptText("请选择路径..");
        HBox.setHgrow(seleTf, Priority.ALWAYS);
        Button seleBtn = new Button("浏览...");
        HBox selehBox = new HBox(10,seleTf,seleBtn);
        VBox vbox1 = new VBox(seleLable,selehBox);

        Label outLable = new Label("请选择输出目录:");
        TextField outTf = new TextField();
        outTf.setPromptText("请选择路径..");
        outTf.setEditable(false);
        HBox.setHgrow(outTf, Priority.ALWAYS);
        Button outBtn = new Button("浏览...");
        HBox outhBox = new HBox(10,outTf,outBtn);
        VBox vbox2 = new VBox(outLable,outhBox);

        Label filePRL = new Label("文件前缀:");
        TextField prTf = new TextField();
        prTf.setPromptText("文件前缀,可不填");
        Label fileNumL = new Label(" + 序号");
        HBox prHbox = new HBox(10, filePRL, prTf, fileNumL);
        prHbox.setAlignment(Pos.CENTER_LEFT);





        Label setPL1 = new Label("每");
        TextField setParamTf = new TextField();
        setParamTf.setPromptText("请输入分割行数");
        Label setPL2 = new Label("行,分割一个文件");
        HBox paramHbox = new HBox(10,setPL1,setParamTf,setPL2);
        paramHbox.setAlignment(Pos.CENTER_LEFT);


        Button startBtn = new Button("开始分割");
        startBtn.setDisable(true);
        Button endBtn = new Button("关闭分割器");
        HBox btnHBox = new HBox(20,startBtn,endBtn);
        btnHBox.setAlignment(Pos.CENTER);
        TextArea infoTa = new TextArea();
        infoTa.setPromptText("信息提示窗");
        infoTa.setEditable(false);




        VBox root = new VBox(10,vbox1,vbox2,paramHbox,prHbox,infoTa,btnHBox);
        root.setPadding(new Insets(10));
        primaryStage.setScene(new Scene(root,400,350));
        primaryStage.setTitle("Txt文件分割器 1.0 Taky QQ:275158188");
        primaryStage.show();

        seleBtn.requestFocus();

        seleBtn.setOnAction(p -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file == null) return;
            if (outBtn.getText().trim().isEmpty() || setParamTf.getText().trim().isEmpty()){
                startBtn.setDisable(true);
            }else {
                startBtn.setDisable(false);
            }
            seleTf.setText(file.getPath());
            srcFile = file;
        });

        outBtn.setOnAction(p -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File file = directoryChooser.showDialog(primaryStage);
            if (file == null) return;
            if (seleTf.getText().trim().isEmpty() || setParamTf.getText().trim().isEmpty()){
                startBtn.setDisable(true);
            }else {
                startBtn.setDisable(false);
            }
            outTf.setText(file.getPath());
            tarFile = file;
        });

        //开始按钮监听
        startBtn.setOnAction(p -> {
            startBtn.setDisable(true);
            startBtn.setText("执行中..");
            infoTa.appendText(LocalDateTime.now() + ": 任务开始!\r\n");
            HandleTask handleTask = new HandleTask();
            handleTask.messageProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    infoTa.appendText(newValue + "\r\n");
                }
            });
            handleTask.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    infoTa.appendText(LocalDateTime.now() + ": 任务完成!");
                    startBtn.setDisable(false);
                    startBtn.setText("开始分割");
                }
            });

            Thread thread = new Thread(handleTask);
            thread.start();
        });

        endBtn.setOnAction(p -> {
            primaryStage.close();
        });

        //前缀监听
        prTf.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                prefixStr = newValue;
            }
        });

        //行数监听
        setParamTf.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.trim().isEmpty() || seleTf.getText().trim().isEmpty() || outTf.getText().trim().isEmpty()) {
                    startBtn.setDisable(true);
                    return;
                }
                startBtn.setDisable(false);
                rowCount = Integer.valueOf(newValue);
            }
        });



    }

    public static void main(String[] args) {
        launch(args);
    }

    class HandleTask extends Task<Number> {

        long start;
        long end;
        int i = 0;
        int j = 0;
        File file;
        List<String> lists = new ArrayList<>();

        @Override
        protected Number call() throws Exception {
            Stream<String> lines = null;
            try {
                lines = Files.lines(srcFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            file = new File(tarFile.getPath() + "/" + prefixStr + j++ + ".txt");
            if (!file.exists()) file.createNewFile();
            start = System.currentTimeMillis();
            lines.forEach(p -> {
                lists.add(p);
                if (i >= rowCount){
                    try {
                        FileUtils.writeLines(file,lists,false);
                        end = System.currentTimeMillis();
                        updateMessage("第" + j + "次分割,耗时: " + (end - start));
                        start = System.currentTimeMillis();
                        i = 0;
                        lists.clear();
                        file = new File(tarFile.getPath() + "/" + prefixStr + j++ + ".txt");
                        if (file.exists()){
                            file.createNewFile();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                i++;
            });
            FileUtils.writeLines(file,lists,false);
            end = System.currentTimeMillis();
            updateMessage("第" + j + "次分割,耗时: " + (end - start));
            return 1;
        }
    }
}
